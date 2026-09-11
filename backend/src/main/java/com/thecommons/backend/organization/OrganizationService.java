package com.thecommons.backend.organization;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Organization createOrganization(String name, String joinCode) {
        String normalizedName = name.trim();

        if (organizationRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new OrganizationAlreadyExistsException(normalizedName);
        }

        String joinCodeHash = passwordEncoder.encode(joinCode);
        return organizationRepository.save(
                new Organization(normalizedName, joinCodeHash));
    }

    @Transactional(readOnly = true)
    public Organization verifyJoinCode(String name, String joinCode) {
        Organization organization = organizationRepository
                .findByNameIgnoreCase(name.trim())
                .orElseThrow(InvalidOrganizationJoinCodeException::new);

        if (!passwordEncoder.matches(joinCode, organization.getJoinCodeHash())) {
            throw new InvalidOrganizationJoinCodeException();
        }

        return organization;
    }
}
