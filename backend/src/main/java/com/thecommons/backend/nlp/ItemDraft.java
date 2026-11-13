package com.thecommons.backend.nlp;

import java.util.List;

/** Suggestions only. Deliberately excludes organization, identifiers, dates and permissions. */
public record ItemDraft(String name, String category, String location, Integer quantity,
        String lastUsed, Boolean shared, List<String> missingRequiredFields) {}
