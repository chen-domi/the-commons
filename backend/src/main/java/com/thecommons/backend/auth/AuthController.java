package com.thecommons.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse getCurrentUser(
            @AuthenticationPrincipal OidcUser user) {
        AppUser appUser = appUserService.getByGoogleSubject(user.getSubject());

        return new AuthenticatedUserResponse(
                user.getFullName(),
                user.getEmail(),
                user.getPicture(),
                AdminPreviewSession.isEffectiveAdmin(appUser) ? GlobalRole.ADMIN : GlobalRole.USER,
                AdminPreviewSession.organization(appUser));
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse getCsrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getToken());
    }
}
