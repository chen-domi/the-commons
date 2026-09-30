package com.thecommons.backend.borrowing;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.auth.AuthenticatedUserNotFoundException;
import com.thecommons.backend.auth.GlobalRole;
import com.thecommons.backend.borrowing.dto.BorrowingNotificationSummary;
import com.thecommons.backend.borrowing.dto.BorrowingRequestResponse;
import com.thecommons.backend.borrowing.dto.CreateBorrowingRequest;
import com.thecommons.backend.inventory.InventoryItem;
import com.thecommons.backend.inventory.InventoryRepository;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;
import com.thecommons.backend.organization.Organization;
import com.thecommons.backend.organization.OrganizationAuthorizationService;
import com.thecommons.backend.organization.OrganizationMembershipRepository;
import com.thecommons.backend.organization.OrganizationNotFoundException;
import com.thecommons.backend.organization.OrganizationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowingRequestService {

    private final BorrowingRequestRepository borrowingRequestRepository;
    private final InventoryRepository inventoryRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationAuthorizationService authorizationService;

    public BorrowingRequestService(
            BorrowingRequestRepository borrowingRequestRepository,
            InventoryRepository inventoryRepository,
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository,
            OrganizationMembershipRepository membershipRepository,
            OrganizationAuthorizationService authorizationService) {
        this.borrowingRequestRepository = borrowingRequestRepository;
        this.inventoryRepository = inventoryRepository;
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public BorrowingRequestResponse create(
            String googleSubject,
            CreateBorrowingRequest request) {
        AppUser user = findUser(googleSubject);
        authorizationService.requireCanManage(
                googleSubject,
                request.borrowingOrganization());

        InventoryItem item = inventoryRepository
                .findById(request.inventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException(
                        request.inventoryItemId()));
        Organization lender = requireOwningOrganization(item);
        Organization borrower = findOrganization(request.borrowingOrganization());

        if (lender.getName().equalsIgnoreCase(borrower.getName())) {
            throw new InvalidBorrowingRequestException(
                    "An organization cannot borrow its own inventory");
        }
        if (!item.isShared()) {
            throw new InvalidBorrowingRequestException(
                    "This inventory item is not available on the marketplace");
        }
        if (request.dueDate().isBefore(request.startDate())) {
            throw new InvalidBorrowingRequestException(
                    "Due date cannot be before the start date");
        }

        int available = availableQuantity(item);
        if (request.quantity() > available) {
            throw new InsufficientInventoryException(request.quantity(), available);
        }

        BorrowingRequest borrowingRequest = new BorrowingRequest(
                item,
                lender,
                borrower,
                user,
                request.quantity(),
                request.purpose().trim(),
                request.startDate(),
                request.dueDate());

        return BorrowingRequestResponse.from(
                borrowingRequestRepository.save(borrowingRequest));
    }

    @Transactional(readOnly = true)
    public List<BorrowingRequestResponse> getVisibleRequests(String googleSubject) {
        AppUser user = findUser(googleSubject);
        authorizationService.requireApplicationAccess(googleSubject);

        List<BorrowingRequest> requests;
        if (user.getGlobalRole() == GlobalRole.ADMIN) {
            requests = borrowingRequestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            List<Organization> organizations = membershipRepository
                    .findAllByUser(user)
                    .stream()
                    .map(membership -> membership.getOrganization())
                    .toList();
            requests = organizations.isEmpty()
                    ? List.of()
                    : borrowingRequestRepository
                            .findAllByLendingOrganizationInOrBorrowingOrganizationInOrderByCreatedAtDesc(
                                    organizations,
                                    organizations);
        }

        return requests.stream()
                .map(BorrowingRequestResponse::from)
                .toList();
    }

    @Transactional
    public BorrowingRequestResponse approve(String googleSubject, Long id) {
        BorrowingRequest request = findRequest(id);
        authorizationService.requireCanManage(
                googleSubject,
                request.getLendingOrganization().getName());
        requireStatus(request, BorrowingRequestStatus.PENDING);

        int available = availableQuantity(request.getInventoryItem());
        if (request.getQuantity() > available) {
            throw new InsufficientInventoryException(request.getQuantity(), available);
        }

        request.approve(findUser(googleSubject));
        return BorrowingRequestResponse.from(
                borrowingRequestRepository.save(request));
    }

    @Transactional
    public BorrowingRequestResponse deny(String googleSubject, Long id) {
        BorrowingRequest request = findRequest(id);
        authorizationService.requireCanManage(
                googleSubject,
                request.getLendingOrganization().getName());
        requireStatus(request, BorrowingRequestStatus.PENDING);
        request.deny(findUser(googleSubject));
        return BorrowingRequestResponse.from(
                borrowingRequestRepository.save(request));
    }

    @Transactional
    public BorrowingRequestResponse cancel(String googleSubject, Long id) {
        BorrowingRequest request = findRequest(id);
        authorizationService.requireCanManage(
                googleSubject,
                request.getBorrowingOrganization().getName());
        requireStatus(request, BorrowingRequestStatus.PENDING);
        request.cancel();
        return BorrowingRequestResponse.from(
                borrowingRequestRepository.save(request));
    }

    @Transactional
    public BorrowingRequestResponse markReturned(String googleSubject, Long id) {
        BorrowingRequest request = findRequest(id);
        authorizationService.requireCanManage(
                googleSubject,
                request.getLendingOrganization().getName());
        requireStatus(request, BorrowingRequestStatus.APPROVED);
        request.markReturned(findUser(googleSubject));
        return BorrowingRequestResponse.from(
                borrowingRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public BorrowingNotificationSummary getNotificationSummary(String googleSubject) {
        AppUser user = findUser(googleSubject);
        authorizationService.requireApplicationAccess(googleSubject);

        if (user.getGlobalRole() == GlobalRole.ADMIN) {
            return new BorrowingNotificationSummary(
                    borrowingRequestRepository.countByStatus(
                            BorrowingRequestStatus.PENDING),
                    borrowingRequestRepository.countByStatus(
                            BorrowingRequestStatus.APPROVED));
        }

        List<Organization> organizations = membershipRepository
                .findAllByUser(user)
                .stream()
                .map(membership -> membership.getOrganization())
                .toList();
        if (organizations.isEmpty()) {
            return new BorrowingNotificationSummary(0, 0);
        }

        return new BorrowingNotificationSummary(
                borrowingRequestRepository.countByLendingOrganizationInAndStatus(
                        organizations,
                        BorrowingRequestStatus.PENDING),
                borrowingRequestRepository.countByBorrowingOrganizationInAndStatus(
                        organizations,
                        BorrowingRequestStatus.APPROVED));
    }

    private AppUser findUser(String googleSubject) {
        return appUserRepository
                .findByGoogleSubject(googleSubject)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }

    private Organization findOrganization(String name) {
        return organizationRepository
                .findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new OrganizationNotFoundException(name));
    }

    private Organization requireOwningOrganization(InventoryItem item) {
        if (item.getOwningOrganization() == null) {
            throw new InvalidBorrowingRequestException(
                    "This inventory item is not linked to a registered organization");
        }
        return item.getOwningOrganization();
    }

    private BorrowingRequest findRequest(Long id) {
        return borrowingRequestRepository
                .findById(id)
                .orElseThrow(() -> new BorrowingRequestNotFoundException(id));
    }

    private int availableQuantity(InventoryItem item) {
        long approved = borrowingRequestRepository
                .sumQuantityByInventoryItemAndStatus(
                        item,
                        BorrowingRequestStatus.APPROVED);
        return Math.max(0, item.getQuantity() - Math.toIntExact(approved));
    }

    private void requireStatus(
            BorrowingRequest request,
            BorrowingRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new InvalidBorrowingRequestStateException(
                    request.getId(),
                    expected,
                    request.getStatus());
        }
    }
}
