package com.thecommons.backend.organization;

import com.thecommons.backend.auth.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, Long> {

    boolean existsByUserAndOrganization(
            AppUser user,
            Organization organization);

    List<OrganizationMembership> findAllByUser(AppUser user);
}
