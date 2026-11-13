package com.thecommons.backend.auth;

import java.io.Serializable;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Server-owned, session-local permission restriction; never changes a stored role. */
public final class AdminPreviewSession {
    public static final String KEY = AdminPreviewSession.class.getName();

    public record Preview(String subject, String organization) implements Serializable {}

    private AdminPreviewSession() {}

    public static Preview current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        HttpSession session = attributes.getRequest().getSession(false);
        return session == null ? null : (Preview) session.getAttribute(KEY);
    }

    public static String organization(AppUser user) {
        Preview preview = current();
        return user.getGlobalRole() == GlobalRole.ADMIN && preview != null
                && preview.subject().equals(user.getGoogleSubject()) ? preview.organization() : null;
    }

    public static boolean isEffectiveAdmin(AppUser user) {
        return user.getGlobalRole() == GlobalRole.ADMIN && organization(user) == null;
    }
}
