package com.thecommons.backend.organization;

import com.thecommons.backend.auth.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, Long> {

    Optional<OrganizationMembership> findByUserAndOrganization(
            AppUser user,
            Organization organization);

    List<OrganizationMembership> findAllByUser(AppUser user);

    boolean existsByUser(AppUser user);

    boolean existsByUserAndOrganization_NameIgnoreCase(
            AppUser user,
            String organizationName);

    long deleteByUserAndOrganization_NameIgnoreCase(
            AppUser user,
            String organizationName);
}
