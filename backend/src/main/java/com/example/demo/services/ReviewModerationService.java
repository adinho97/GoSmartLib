package com.example.demo.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ReviewModerationService {

    private static final String CENSORED_WORDS_RESOURCE = "censored-words.json";
    private static final Set<String> CENSORED_WORDS = loadCensoredWords();
    private static final Set<String> CENSORED_WORDS_WITH_ONE_MISSING_CHAR = buildOneMissingCharVariants(CENSORED_WORDS);

    public void validateReviewComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return;
        }

        String normalized = comment.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("\\s+");

        for (String token : tokens) {
            String compactToken = normalizeToken(token);
            if (compactToken.isEmpty()) {
                continue;
            }

            boolean hadObfuscationCharacters = !compactToken.equals(token);

            if (isCensoredToken(compactToken, hadObfuscationCharacters)) {
                throw new ApiException(
                        "Je review bevat een niet-toegestaan woord en kan niet worden geplaatst.",
                        HttpStatus.BAD_REQUEST,
                        "REVIEW_CONTAINS_CENSORED_WORD");
            }
        }
    }

    private static Set<String> loadCensoredWords() {
        try (InputStream in = ReviewModerationService.class.getClassLoader()
                .getResourceAsStream(CENSORED_WORDS_RESOURCE)) {
            if (in == null) {
                return Collections.emptySet();
            }

            List<String> rawWords = new ObjectMapper().readValue(in, new TypeReference<>() {
            });

            Set<String> normalizedWords = new HashSet<>();
            for (String word : rawWords) {
                if (word == null) {
                    continue;
                }
                String normalizedWord = normalizeToken(word.trim().toLowerCase(Locale.ROOT));
                if (!normalizedWord.isEmpty()) {
                    normalizedWords.add(normalizedWord);
                }
            }

            return Collections.unmodifiableSet(normalizedWords);
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
    }

    private static Set<String> buildOneMissingCharVariants(Set<String> words) {
        Set<String> variants = new HashSet<>();

        for (String word : words) {
            // Keep shorter words strict to reduce false positives.
            if (word.length() < 5) {
                continue;
            }

            for (int i = 0; i < word.length(); i++) {
                String variant = word.substring(0, i) + word.substring(i + 1);
                if (!variant.isEmpty()) {
                    variants.add(variant);
                }
            }
        }

        return Collections.unmodifiableSet(variants);
    }

    private static String normalizeToken(String token) {
        // First map common Unicode confusables (homoglyphs) to ASCII equivalents
        String mapped = normalizeConfusables(token);

        // Then remove any remaining non-letters/digits
        return mapped.replaceAll("[^\\p{L}\\p{Nd}]", "");
    }

    private static String normalizeConfusables(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Character mapped = CONFUSABLES.get(c);
            if (mapped != null) {
                sb.append(mapped.charValue());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Minimal confusables map: extend as needed. Covers common Cyrillic/Armenian/Greek lookalikes.
    private static final java.util.Map<Character, Character> CONFUSABLES = buildConfusables();

    private static java.util.Map<Character, Character> buildConfusables() {
        java.util.Map<Character, Character> m = new java.util.HashMap<>();

        // Cyrillic -> Latin
        m.put('\u0430', 'a'); // а
        m.put('\u0410', 'a'); // А
        m.put('\u0441', 'c'); // с -> c
        m.put('\u0421', 'c'); // С
        m.put('\u0456', 'i'); // і -> i
        m.put('\u0406', 'i'); // І
        m.put('\u0435', 'e'); // е
        m.put('\u0415', 'e'); // Е
        m.put('\u043E', 'o'); // О
        m.put('\u043E', 'o'); // о
        m.put('\u0440', 'p'); // р -> p
        m.put('\u0445', 'x'); // х -> x
        m.put('\u043A', 'k'); // к -> k

        // Greek -> Latin
        m.put('\u03B1', 'a'); // α
        m.put('\u03BF', 'o'); // ο
        m.put('\u03C1', 'p'); // ρ

        // Armenian common homoglyphs (covering examples like 'ս')
        m.put('\u057D', 'u'); // ս -> map to 'u' (handles some obfuscations visually similar to 'u')
        m.put('\u0561', 'a'); // ա -> a

        // Fullwidth forms and some symbols
        m.put('\uFF21', 'a'); // fullwidth A -> treat as ascii after normalization; adding few examples

        // Common lookalike letters often used in obfuscation
        m.put('\u24D0', 'a'); // ⓐ -> a

        return java.util.Collections.unmodifiableMap(m);
    }

    private static boolean isCensoredToken(String compactToken, boolean hadObfuscationCharacters) {
        if (CENSORED_WORDS.contains(compactToken)) {
            return true;
        }

        if (hadObfuscationCharacters && CENSORED_WORDS_WITH_ONE_MISSING_CHAR.contains(compactToken)) {
            return true;
        }

        // Check if the token is composed of multiple censored words concatenated together
        if (containsCompoundCensoredWords(compactToken)) {
            return true;
        }

        // Check if any censored word is hidden within the token as a substring
        if (containsCensoredWordAsSubstring(compactToken)) {
            return true;
        }

        return false;
    }

    private static boolean containsCensoredWordAsSubstring(String token) {
        // Check if any censored word appears as a substring in the token
        // But only flag if the word is at least 3 characters to reduce false positives
        for (String censoredWord : CENSORED_WORDS) {
            // Only check words that are reasonably long to avoid false positives with common substrings
            if (censoredWord.length() >= 3 && token.contains(censoredWord)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCompoundCensoredWords(String token) {
        // Use dynamic programming to check if token can be decomposed into censored words
        int n = token.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true; // Empty string can be formed

        for (int i = 1; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j]) {
                    String substring = token.substring(j, i);
                    if (CENSORED_WORDS.contains(substring)) {
                        dp[i] = true;
                        break;
                    }
                }
            }
        }

        // Return true only if we found multiple words (at least 2)
        // To avoid false positives with single censored words
        return dp[n] && hasMultipleCensoredWords(token);
    }

    private static boolean hasMultipleCensoredWords(String token) {
        // Check if token contains at least 2 censored words
        int count = 0;
        int i = 0;
        int n = token.length();

        while (i < n) {
            boolean found = false;
            // Try to match the longest censored word first to avoid false matches
            for (int j = n; j > i; j--) {
                String substring = token.substring(i, j);
                if (CENSORED_WORDS.contains(substring)) {
                    count++;
                    i = j;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // If we can't match any censored word, it's not a compound word
                return false;
            }
        }

        // Only flag if we found 2 or more censored words
        return count >= 2;
    }
}
