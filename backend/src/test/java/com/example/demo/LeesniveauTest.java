package com.example.demo;

import com.example.demo.entities.Leesniveau;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeesniveauTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromValueReturnsMatchingEnumForKnownLabel() {
        Leesniveau result = Leesniveau.fromValue("1ste-2de leerljaar");

        assertEquals(Leesniveau.EERSTE_TWEEDE_LEERJAAR, result);
    }

    @Test
    void fromValueReturnsNullForUnknownOrNullLabel() {
        assertNull(Leesniveau.fromValue("onbekend"));
        assertNull(Leesniveau.fromValue(null));
    }

    @Test
    void getLabelReturnsApiValue() {
        assertEquals("3de-4de leerjaar", Leesniveau.DERDE_VIERDE_LEERJAAR.getLabel());
    }

    @Test
    void jacksonSerializesAndDeserializesUsingLabel() throws Exception {
        String serialized = objectMapper.writeValueAsString(Leesniveau.EERSTE_GRAAD);
        Leesniveau deserialized = objectMapper.readValue("\"1ste graad\"", Leesniveau.class);

        assertEquals("\"1ste graad\"", serialized);
        assertEquals(Leesniveau.EERSTE_GRAAD, deserialized);
    }
}
