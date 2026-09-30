package com.thecommons.backend.borrowing;

import com.thecommons.backend.inventory.InventoryItem;
import com.thecommons.backend.organization.Organization;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowingRequestRepository
        extends JpaRepository<BorrowingRequest, Long> {

    List<BorrowingRequest>
            findAllByLendingOrganizationInOrBorrowingOrganizationInOrderByCreatedAtDesc(
                    Collection<Organization> lendingOrganizations,
                    Collection<Organization> borrowingOrganizations);

    long countByLendingOrganizationInAndStatus(
            Collection<Organization> organizations,
            BorrowingRequestStatus status);

    long countByBorrowingOrganizationInAndStatus(
            Collection<Organization> organizations,
            BorrowingRequestStatus status);

    @Query("""
            select coalesce(sum(request.quantity), 0)
            from BorrowingRequest request
            where request.inventoryItem = :item
              and request.status = com.thecommons.backend.borrowing.BorrowingRequestStatus.APPROVED
            """)
    Long sumApprovedQuantityByInventoryItem(@Param("item") InventoryItem item);
}
