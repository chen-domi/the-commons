package com.thecommons.backend.organization;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository
        extends JpaRepository<Organization, Long> {

    Optional<Organization> findByNameIgnoreCase(String name);
}
