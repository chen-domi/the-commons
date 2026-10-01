package com.thecommons.backend.borrowing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecommons.backend.borrowing.dto.BorrowingNotificationSummary;
import com.thecommons.backend.borrowing.dto.BorrowingRequestResponse;
import com.thecommons.backend.borrowing.dto.CreateBorrowingRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = BorrowingRequestController.class,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration,"
                + "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration")
@AutoConfigureMockMvc(addFilters = false)
class BorrowingRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BorrowingRequestService borrowingRequestService;

    @Test
    void getRequestsReturnsVisibleRequests() throws Exception {
        when(borrowingRequestService.getVisibleRequests("subject"))
                .thenReturn(List.of(response(BorrowingRequestStatus.PENDING)));

        mockMvc.perform(get("/api/borrowing-requests").principal(() -> "subject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Table"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getNotificationsReturnsRealCounts() throws Exception {
        when(borrowingRequestService.getNotificationSummary("subject"))
                .thenReturn(new BorrowingNotificationSummary(3, 1));

        mockMvc.perform(get("/api/borrowing-requests/notifications")
                        .principal(() -> "subject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingIncomingRequests").value(3))
                .andExpect(jsonPath("$.approvedOutgoingBorrows").value(1));
    }

    @Test
    void createReturnsCreatedRequest() throws Exception {
        when(borrowingRequestService.create(
                eq("subject"), any(CreateBorrowingRequest.class)))
                .thenReturn(response(BorrowingRequestStatus.PENDING));

        mockMvc.perform(post("/api/borrowing-requests")
                        .principal(() -> "subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.borrowingOrganization").value("Borrower"));
    }

    @Test
    void createRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/borrowing-requests")
                        .principal(() -> "subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inventoryItemId": 1,
                                  "borrowingOrganization": "",
                                  "quantity": 0,
                                  "purpose": "",
                                  "startDate": "2000-01-01",
                                  "dueDate": "2000-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void lifecycleEndpointsDelegateToService() throws Exception {
        when(borrowingRequestService.approve("subject", 1L))
                .thenReturn(response(BorrowingRequestStatus.APPROVED));
        when(borrowingRequestService.deny("subject", 2L))
                .thenReturn(response(BorrowingRequestStatus.DENIED));
        when(borrowingRequestService.cancel("subject", 3L))
                .thenReturn(response(BorrowingRequestStatus.CANCELLED));
        when(borrowingRequestService.markReturned("subject", 4L))
                .thenReturn(response(BorrowingRequestStatus.RETURNED));

        mockMvc.perform(patch("/api/borrowing-requests/1/approve")
                        .principal(() -> "subject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(patch("/api/borrowing-requests/2/deny")
                        .principal(() -> "subject"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/borrowing-requests/3/cancel")
                        .principal(() -> "subject"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/borrowing-requests/4/return")
                        .principal(() -> "subject"))
                .andExpect(status().isOk());

        verify(borrowingRequestService).approve("subject", 1L);
        verify(borrowingRequestService).deny("subject", 2L);
        verify(borrowingRequestService).cancel("subject", 3L);
        verify(borrowingRequestService).markReturned("subject", 4L);
    }

    private String validCreateJson() {
        return """
                {
                  "inventoryItemId": 1,
                  "borrowingOrganization": "Borrower",
                  "quantity": 2,
                  "purpose": "Student event",
                  "startDate": "2099-01-01",
                  "dueDate": "2099-01-03"
                }
                """;
    }

    private BorrowingRequestResponse response(BorrowingRequestStatus status) {
        return new BorrowingRequestResponse(
                1L,
                1L,
                "Table",
                "QR-1",
                "Lender",
                "Borrower",
                2,
                "Student event",
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                status,
                "Requester",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null,
                null);
    }
}
