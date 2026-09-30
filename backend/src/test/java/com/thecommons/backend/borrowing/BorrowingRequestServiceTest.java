package com.thecommons.backend.borrowing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.borrowing.dto.BorrowingRequestResponse;
import com.thecommons.backend.borrowing.dto.CreateBorrowingRequest;
import com.thecommons.backend.inventory.InventoryItem;
import com.thecommons.backend.inventory.InventoryRepository;
import com.thecommons.backend.organization.Organization;
import com.thecommons.backend.organization.OrganizationAuthorizationService;
import com.thecommons.backend.organization.OrganizationMembershipRepository;
import com.thecommons.backend.organization.OrganizationRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BorrowingRequestServiceTest {

    @Mock private BorrowingRequestRepository borrowingRequestRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private OrganizationAuthorizationService authorizationService;

    private BorrowingRequestService service;
    private AppUser requester;
    private Organization lender;
    private Organization borrower;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        service = new BorrowingRequestService(
                borrowingRequestRepository,
                inventoryRepository,
                organizationRepository,
                appUserRepository,
                membershipRepository,
                authorizationService);
        requester = new AppUser("requester-subject", "requester@bc.edu", "Requester");
        lender = new Organization("Lender", "hash");
        borrower = new Organization("Borrower", "hash");
        item = new InventoryItem(
                "QR-1", "Table", "Furniture", "Lender", "Storage", 5);
        item.setOwningOrganization(lender);
        item.setShared(true);
    }

    @Test
    void createSavesValidPartialQuantityRequest() {
        CreateBorrowingRequest request = createRequest(2);
        when(appUserRepository.findByGoogleSubject("requester-subject"))
                .thenReturn(Optional.of(requester));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(organizationRepository.findByNameIgnoreCase("Borrower"))
                .thenReturn(Optional.of(borrower));
        when(borrowingRequestRepository.sumQuantityByInventoryItemAndStatus(
                item, BorrowingRequestStatus.APPROVED)).thenReturn(1L);
        when(borrowingRequestRepository.save(any(BorrowingRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowingRequestResponse result = service.create(
                "requester-subject", request);

        assertEquals(2, result.quantity());
        assertEquals("Lender", result.lendingOrganization());
        assertEquals("Borrower", result.borrowingOrganization());
        assertEquals(BorrowingRequestStatus.PENDING, result.status());
        verify(authorizationService).requireCanManage(
                "requester-subject", "Borrower");
    }

    @Test
    void createRejectsRequestForOrganizationsOwnItem() {
        CreateBorrowingRequest request = new CreateBorrowingRequest(
                1L, "Lender", 1, "Event", date(1), date(2));
        when(appUserRepository.findByGoogleSubject("requester-subject"))
                .thenReturn(Optional.of(requester));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(organizationRepository.findByNameIgnoreCase("Lender"))
                .thenReturn(Optional.of(lender));

        assertThrows(
                InvalidBorrowingRequestException.class,
                () -> service.create("requester-subject", request));
        verify(borrowingRequestRepository, never()).save(any());
    }

    @Test
    void createRejectsQuantityAboveCurrentAvailability() {
        when(appUserRepository.findByGoogleSubject("requester-subject"))
                .thenReturn(Optional.of(requester));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(organizationRepository.findByNameIgnoreCase("Borrower"))
                .thenReturn(Optional.of(borrower));
        when(borrowingRequestRepository.sumQuantityByInventoryItemAndStatus(
                item, BorrowingRequestStatus.APPROVED)).thenReturn(4L);

        assertThrows(
                InsufficientInventoryException.class,
                () -> service.create("requester-subject", createRequest(2)));
    }

    @Test
    void approveRechecksAvailabilityAndApprovesPendingRequest() {
        BorrowingRequest request = borrowingRequest(2);
        when(borrowingRequestRepository.findById(10L))
                .thenReturn(Optional.of(request));
        when(inventoryRepository.findByIdForUpdate(item.getId()))
                .thenReturn(Optional.of(item));
        when(borrowingRequestRepository.sumQuantityByInventoryItemAndStatus(
                item, BorrowingRequestStatus.APPROVED)).thenReturn(2L);
        when(appUserRepository.findByGoogleSubject("reviewer-subject"))
                .thenReturn(Optional.of(requester));
        when(borrowingRequestRepository.save(request)).thenReturn(request);

        BorrowingRequestResponse result = service.approve("reviewer-subject", 10L);

        assertEquals(BorrowingRequestStatus.APPROVED, result.status());
        verify(authorizationService).requireCanManage(
                "reviewer-subject", "Lender");
    }

    @Test
    void approveRejectsRequestWhenAvailabilityChanged() {
        BorrowingRequest request = borrowingRequest(2);
        when(borrowingRequestRepository.findById(10L))
                .thenReturn(Optional.of(request));
        when(inventoryRepository.findByIdForUpdate(item.getId()))
                .thenReturn(Optional.of(item));
        when(borrowingRequestRepository.sumQuantityByInventoryItemAndStatus(
                item, BorrowingRequestStatus.APPROVED)).thenReturn(4L);

        assertThrows(
                InsufficientInventoryException.class,
                () -> service.approve("reviewer-subject", 10L));
        verify(borrowingRequestRepository, never()).save(any());
    }

    @Test
    void cancelChangesBorrowersPendingRequestToCancelled() {
        BorrowingRequest request = borrowingRequest(1);
        when(borrowingRequestRepository.findById(10L))
                .thenReturn(Optional.of(request));
        when(borrowingRequestRepository.save(request)).thenReturn(request);

        BorrowingRequestResponse result = service.cancel("requester-subject", 10L);

        assertEquals(BorrowingRequestStatus.CANCELLED, result.status());
        verify(authorizationService).requireCanManage(
                "requester-subject", "Borrower");
    }

    private CreateBorrowingRequest createRequest(int quantity) {
        return new CreateBorrowingRequest(
                1L, "Borrower", quantity, "Student event", date(1), date(3));
    }

    private BorrowingRequest borrowingRequest(int quantity) {
        return new BorrowingRequest(
                item,
                lender,
                borrower,
                requester,
                quantity,
                "Student event",
                date(1),
                date(3));
    }

    private LocalDate date(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow);
    }
}
