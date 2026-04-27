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
        Leesniveau result = Leesniveau.fromValue("A");

        assertEquals(Leesniveau.A, result);
    }

    @Test
    void fromValueReturnsNullForUnknownOrNullLabel() {
        assertNull(Leesniveau.fromValue("onbekend"));
        assertNull(Leesniveau.fromValue(null));
    }

    @Test
    void getLabelReturnsApiValue() {
        assertEquals("B", Leesniveau.B.getLabel());
    }

    @Test
    void jacksonSerializesAndDeserializesUsingLabel() throws Exception {
        String serialized = objectMapper.writeValueAsString(Leesniveau.C);
        Leesniveau deserialized = objectMapper.readValue("\"C\"", Leesniveau.class);

        assertEquals("\"C\"", serialized);
        assertEquals(Leesniveau.C, deserialized);
    }
}
