package com.thecommons.backend.auth;

import java.util.Locale;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser synchronizeUser(OidcUser oidcUser) {
        String googleSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail().toLowerCase(Locale.ROOT);
        String name = oidcUser.getFullName();

        AppUser appUser = appUserRepository
                .findByGoogleSubject(googleSubject)
                .orElseGet(() -> new AppUser(googleSubject, email, name));

        appUser.updateProfile(email, name);
        return appUserRepository.save(appUser);
    }

    @Transactional(readOnly = true)
    public AppUser getByGoogleSubject(String googleSubject) {
        return appUserRepository.findByGoogleSubject(googleSubject)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }
}
