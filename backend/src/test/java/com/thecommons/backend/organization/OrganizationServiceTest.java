package com.thecommons.backend.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(
                organizationRepository,
                passwordEncoder);
    }

    @Test
    void getAllOrganizationsReturnsOrganizationsByName() {
        List<Organization> organizations = List.of(
                new Organization("Another Club", "first-hash"),
                new Organization("UGBC", "second-hash"));
        when(organizationRepository.findAllByOrderByNameAsc())
                .thenReturn(organizations);

        List<Organization> result = organizationService.getAllOrganizations();

        assertSame(organizations, result);
        verify(organizationRepository).findAllByOrderByNameAsc();
    }

    @Test
    void createOrganizationHashesJoinCodeBeforeSaving() {
        when(organizationRepository.findByNameIgnoreCase("UGBC"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("hashed-code");
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Organization result = organizationService
                .createOrganization(" UGBC ", "1234");

        assertEquals("UGBC", result.getName());
        assertEquals("hashed-code", result.getJoinCodeHash());
        verify(passwordEncoder).encode("1234");
        verify(organizationRepository).save(result);
    }

    @Test
    void createOrganizationRejectsDuplicateName() {
        Organization existing = new Organization("UGBC", "existing-hash");
        when(organizationRepository.findByNameIgnoreCase("UGBC"))
                .thenReturn(Optional.of(existing));

        assertThrows(
                OrganizationAlreadyExistsException.class,
                () -> organizationService.createOrganization("UGBC", "1234"));

        verifyNoInteractions(passwordEncoder);
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void verifyJoinCodeReturnsOrganizationWhenCodeMatches() {
        Organization organization = new Organization("UGBC", "stored-hash");
        when(organizationRepository.findByNameIgnoreCase("UGBC"))
                .thenReturn(Optional.of(organization));
        when(passwordEncoder.matches("1234", "stored-hash"))
                .thenReturn(true);

        Organization result = organizationService.verifyJoinCode("UGBC", "1234");

        assertSame(organization, result);
    }

    @Test
    void verifyJoinCodeRejectsIncorrectCode() {
        Organization organization = new Organization("UGBC", "stored-hash");
        when(organizationRepository.findByNameIgnoreCase("UGBC"))
                .thenReturn(Optional.of(organization));
        when(passwordEncoder.matches("wrong", "stored-hash"))
                .thenReturn(false);

        assertThrows(
                InvalidOrganizationJoinCodeException.class,
                () -> organizationService.verifyJoinCode("UGBC", "wrong"));
    }

    @Test
    void verifyJoinCodeRejectsUnknownOrganization() {
        when(organizationRepository.findByNameIgnoreCase("Unknown Club"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidOrganizationJoinCodeException.class,
                () -> organizationService.verifyJoinCode(
                        "Unknown Club",
                        "1234"));

        verifyNoInteractions(passwordEncoder);
    }
}
