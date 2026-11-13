package com.thecommons.backend.nlp;

import com.thecommons.backend.organization.OrganizationAuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class ItemDescriptionService {
    private final OrganizationAuthorizationService authorization;
    private final ItemDescriptionModel model;
    private final ItemDraftParser parser;
    private final NlpRateLimiter limiter;

    public ItemDescriptionService(OrganizationAuthorizationService authorization,
            ItemDescriptionModel model, ItemDraftParser parser, NlpRateLimiter limiter) {
        this.authorization = authorization;
        this.model = model;
        this.parser = parser;
        this.limiter = limiter;
    }

    public boolean isAvailable() { return model.isAvailable(); }

    public ItemDraft parse(String subject, String organization, String description) {
        authorization.requireCanManage(subject, organization);
        if (!isAvailable()) throw NlpException.unavailable();
        limiter.acquire(subject);
        try {
            return parser.parse(model.extract(description.trim()));
        } finally {
            limiter.release();
        }
    }
}
