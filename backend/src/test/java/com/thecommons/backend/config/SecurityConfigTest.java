package com.thecommons.backend.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecommons.backend.auth.BcOidcUserService;
import com.thecommons.backend.inventory.InventoryController;
import com.thecommons.backend.inventory.InventoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = InventoryController.class,
        properties = {
                "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
        })
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private BcOidcUserService bcOidcUserService;

    @Test
    void getInventoryIsPublic() throws Exception {
        when(inventoryService.getAllItems()).thenReturn(List.of());

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void createInventoryRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/inventory").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateInventoryRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/inventory/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteInventoryRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/inventory/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inventoryMutationWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/inventory"))
                .andExpect(status().isForbidden());
    }
}
