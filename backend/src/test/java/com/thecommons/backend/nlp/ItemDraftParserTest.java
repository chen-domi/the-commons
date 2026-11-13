package com.thecommons.backend.nlp;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

class ItemDraftParserTest {
    private final ItemDraftParser parser = new ItemDraftParser();

    @Test void parsesCompleteDraft() {
        var draft = parser.parse("""
                {"name":" Folding chairs ","category":"Furniture","location":"CAB closet",
                 "quantity":12,"lastUsed":"Club fair","shared":true}
                """);
        assertEquals("Folding chairs", draft.name());
        assertEquals(12, draft.quantity());
        assertEquals(true, draft.shared());
        assertEquals(List.of(), draft.missingRequiredFields());
    }

    @Test void missingDetailsStayNullInsteadOfBeingInvented() {
        var draft = parser.parse("{\"name\":\"Chairs\"}");
        assertNull(draft.quantity());
        assertNull(draft.location());
        assertNull(draft.shared());
        assertEquals(List.of("category", "location", "quantity"), draft.missingRequiredFields());
    }

    @Test void falseSharingPreferenceIsPreserved() {
        assertEquals(false, parser.parse("{\"name\":\"Chairs\",\"shared\":false}").shared());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not json", "```json\n{}\n```", "[]", "null", "{}", "{\"name\":null}",
        "{\"name\":\"x\",\"organization\":\"Other\"}",
        "{\"name\":\"x\",\"qrCode\":\"override\"}", "{\"globalRole\":\"ADMIN\"}",
        "{\"name\":\"x\",\"category\":\"Unknown\"}", "{\"name\":true}",
        "{\"name\":\"x\",\"quantity\":0}", "{\"name\":\"x\",\"quantity\":-1}",
        "{\"name\":\"x\",\"quantity\":1.5}", "{\"name\":\"x\",\"quantity\":\"12\"}",
        "{\"name\":\"x\",\"quantity\":999999999999}", "{\"name\":\"x\",\"quantity\":100001}",
        "{\"name\":\"x\",\"shared\":\"true\"}", "{\"name\":\"x\",\"name\":\"y\"}",
        "{\"name\":\"x\"} {\"name\":\"y\"}", "{\"name\":\"x\\nother\"}"
    })
    void rejectsMalformedOrUnsafeFields(String json) {
        var error = assertThrows(NlpException.class, () -> parser.parse(json));
        assertEquals(502, error.status().value());
    }

    @Test void rejectsOversizedOutputAndFields() {
        assertThrows(NlpException.class, () -> parser.parse("x".repeat(4097)));
        assertThrows(NlpException.class, () -> parser.parse("{\"name\":\"" + "x".repeat(151) + "\"}"));
    }
}
