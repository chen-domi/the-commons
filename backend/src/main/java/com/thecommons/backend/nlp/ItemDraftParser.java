package com.thecommons.backend.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ItemDraftParser {
    public static final List<String> CATEGORIES = List.of(
            "Furniture", "AV Equipment", "Signage", "Outdoor", "Décor", "Safety", "Sustainability");
    private static final Set<String> FIELDS = Set.of("name", "category", "location", "quantity", "lastUsed", "shared");
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public ItemDraft parse(String response) {
        try {
            if (response == null || response.length() > 4096) throw NlpException.invalidResponse();
            JsonNode root = mapper.readTree(response);
            if (root == null || !root.isObject()) throw NlpException.invalidResponse();
            for (String field : root.propertyNames()) {
                if (!FIELDS.contains(field)) throw NlpException.invalidResponse();
            }
            String name = text(root, "name", 150);
            String category = text(root, "category", 50);
            String location = text(root, "location", 255);
            String lastUsed = text(root, "lastUsed", 255);
            if (category != null && !CATEGORIES.contains(category)) throw NlpException.invalidResponse();
            Integer quantity = null;
            JsonNode value = root.get("quantity");
            if (value != null && !value.isNull()) {
                if (!value.isIntegralNumber() || !value.canConvertToInt()
                        || value.intValue() < 1 || value.intValue() > 100_000) throw NlpException.invalidResponse();
                quantity = value.intValue();
            }
            Boolean shared = null;
            value = root.get("shared");
            if (value != null && !value.isNull()) {
                if (!value.isBoolean()) throw NlpException.invalidResponse();
                shared = value.booleanValue();
            }
            if (name == null && category == null && location == null && quantity == null
                    && lastUsed == null && shared == null) throw NlpException.invalidResponse();
            List<String> missing = new ArrayList<>();
            if (name == null) missing.add("name");
            if (category == null) missing.add("category");
            if (location == null) missing.add("location");
            if (quantity == null) missing.add("quantity");
            return new ItemDraft(name, category, location, quantity, lastUsed, shared, List.copyOf(missing));
        } catch (NlpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // Never return raw model output, provider details or stack traces to clients.
            throw NlpException.invalidResponse();
        }
    }

    private String text(JsonNode root, String field, int maximum) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isString()) throw NlpException.invalidResponse();
        String text = value.stringValue().trim();
        if (text.length() > maximum || text.codePoints().anyMatch(Character::isISOControl)) {
            throw NlpException.invalidResponse();
        }
        return text.isEmpty() ? null : text;
    }
}
