package com.thecommons.backend.nlp;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.thecommons.backend.auth.BcOidcUserService;
import com.thecommons.backend.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ItemDescriptionController.class, properties = {
    "spring.security.oauth2.client.registration.google.client-id=test",
    "spring.security.oauth2.client.registration.google.client-secret=test"
})
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class ItemDescriptionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ItemDescriptionService service;
    @MockitoBean BcOidcUserService oidc;
    private static final String PATH = "/api/inventory/parse-description";
    private static final String BODY = "{\"description\":\"12 chairs\",\"organization\":\"CAB\"}";

    @Test void requiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(post(PATH).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(oidcLogin()).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(get("/api/nlp/status")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void validatesInputBeforeService() throws Exception {
        for (String description : List.of("", "x".repeat(2001))) {
            mvc.perform(post(PATH).with(oidcLogin()).with(csrf()).contentType("application/json")
                    .content("{\"description\":\"" + description + "\",\"organization\":\"CAB\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void returnsSuggestionsNotAnInventoryRecord() throws Exception {
        when(service.parse("subject", "CAB", "12 chairs")).thenReturn(new ItemDraft(
                "Chairs", "Furniture", null, 12, null, null, List.of("location")));
        mvc.perform(post(PATH).with(oidcLogin().idToken(token -> token.subject("subject")))
                .with(csrf()).contentType("application/json").content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.missingRequiredFields[0]").value("location"))
                .andExpect(jsonPath("$.organization").doesNotExist()).andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test void disabledProviderReturnsSafeError() throws Exception {
        when(service.parse(any(), any(), any())).thenThrow(NlpException.unavailable());
        mvc.perform(post(PATH).with(oidcLogin()).with(csrf()).contentType("application/json").content(BODY))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("NLP_REQUEST_FAILED"));
    }
}
