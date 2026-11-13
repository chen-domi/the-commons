package com.thecommons.backend.auth;

import com.thecommons.backend.organization.OrganizationNotFoundException;
import com.thecommons.backend.organization.OrganizationRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/preview")
public class AdminPreviewController {
    private final AppUserService users;
    private final OrganizationRepository organizations;

    public AdminPreviewController(AppUserService users, OrganizationRepository organizations) {
        this.users = users;
        this.organizations = organizations;
    }

    public record PreviewRequest(@NotBlank String organizationName) {}

    private void requireAdmin(OidcUser principal) {
        if (users.getByGoogleSubject(principal.getSubject()).getGlobalRole() != GlobalRole.ADMIN) {
            throw new AccessDeniedException("Only administrators can use e-board preview");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody PreviewRequest request, HttpSession session) {
        requireAdmin(principal);
        var organization = organizations.findByNameIgnoreCase(request.organizationName().trim())
                .orElseThrow(() -> new OrganizationNotFoundException(request.organizationName()));
        session.setAttribute(AdminPreviewSession.KEY,
                new AdminPreviewSession.Preview(principal.getSubject(), organization.getName()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@AuthenticationPrincipal OidcUser principal, HttpSession session) {
        requireAdmin(principal);
        session.removeAttribute(AdminPreviewSession.KEY);
    }
}
