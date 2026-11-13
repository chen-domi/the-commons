package com.thecommons.backend.nlp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.thecommons.backend.organization.OrganizationAuthorizationService;
import com.thecommons.backend.organization.OrganizationService;
import com.thecommons.backend.organization.OrganizationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ItemDescriptionServiceTest {
    private final OrganizationAuthorizationService authorization = mock(OrganizationAuthorizationService.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final ItemDescriptionModel model = mock(ItemDescriptionModel.class);
    private ItemDescriptionService service;

    @BeforeEach void setup() {
        service = new ItemDescriptionService(authorization, model, new ItemDraftParser(), new NlpRateLimiter(), organizations);
    }

    @Test void checksPermissionBeforeAnyAiInteraction() {
        doThrow(new AccessDeniedException("denied")).when(authorization).requireCanManage("subject", "Other");
        assertThrows(AccessDeniedException.class, () -> service.parse("subject", "Other", "chairs"));
        verifyNoInteractions(model, organizations);
    }

    @Test void requiresRegisteredOrganizationBeforeCallingAi() {
        when(organizations.getByName("Unknown")).thenThrow(new OrganizationNotFoundException("Unknown"));
        assertThrows(OrganizationNotFoundException.class, () -> service.parse("subject", "Unknown", "chairs"));
        verifyNoInteractions(model);
    }

    @Test void disabledFeatureDoesNotCallProvider() {
        assertEquals(503, assertThrows(NlpException.class, () -> service.parse("s", "CAB", "chairs")).status().value());
        verify(model, never()).extract(any());
    }

    @Test void returnsDraftAndOnlySendsDescriptionNotOrganizationOrIdentity() {
        when(model.isAvailable()).thenReturn(true);
        when(model.extract("12 chairs")).thenReturn("{\"name\":\"Chairs\",\"quantity\":12}");
        var draft = service.parse("subject", "CAB", " 12 chairs ");
        assertEquals(12, draft.quantity());
        verify(model).extract("12 chairs");
    }

    @Test void providerFailureReleasesConcurrencySlot() {
        when(model.isAvailable()).thenReturn(true);
        when(model.extract(any())).thenThrow(NlpException.unavailable()).thenReturn("{\"name\":\"Chairs\"}");
        assertThrows(NlpException.class, () -> service.parse("s", "CAB", "chairs"));
        assertDoesNotThrow(() -> service.parse("s", "CAB", "chairs"));
    }
}
