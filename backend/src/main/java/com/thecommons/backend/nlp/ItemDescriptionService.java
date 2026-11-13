package com.thecommons.backend.nlp;

import com.thecommons.backend.organization.OrganizationAuthorizationService;
import com.thecommons.backend.organization.OrganizationService;
import org.springframework.stereotype.Service;

@Service
public class ItemDescriptionService {
    private final OrganizationAuthorizationService authorization;
    private final ItemDescriptionModel model;
    private final ItemDraftParser parser;
    private final NlpRateLimiter limiter;
    private final OrganizationService organizations;

    public ItemDescriptionService(OrganizationAuthorizationService authorization,
            ItemDescriptionModel model, ItemDraftParser parser, NlpRateLimiter limiter,
            OrganizationService organizations) {
        this.authorization = authorization;
        this.model = model;
        this.parser = parser;
        this.limiter = limiter;
        this.organizations = organizations;
    }

    public boolean isAvailable() { return model.isAvailable(); }

    public ItemDraft parse(String subject, String organization, String description) {
        authorization.requireCanManage(subject, organization);
        organizations.getByName(organization);
        if (!isAvailable()) throw NlpException.unavailable();
        limiter.acquire(subject);
        try {
            return parser.parse(model.extract(description.trim()));
        } finally {
            limiter.release();
        }
    }
}
