package com.example.demo;

import com.example.demo.exception.ApiException;
import com.example.demo.services.ReviewModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ReviewModerationServiceTest {

    private final ReviewModerationService reviewModerationService = new ReviewModerationService();

    @Test
    void validateReviewCommentShouldAllowCleanComments() {
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment("Dit is een heel goed boek!"));
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment("Ik vond het fantastisch"));
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment("Aanbevolen voor iedereen"));
    }

    @Test
    void validateReviewCommentShouldAllowNullAndBlankComments() {
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment(null));
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment(""));
        assertDoesNotThrow(() -> reviewModerationService.validateReviewComment("   "));
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForDirectCensoredWords() {
        ApiException exception = assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is bullshit")
        );
        assertEquals("Je review bevat een niet-toegestaan woord en kan niet worden geplaatst.", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("REVIEW_CONTAINS_CENSORED_WORD", exception.getCode());
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordsIgnoringCase() {
        ApiException exception = assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is BULLSHIT")
        );
        assertEquals("REVIEW_CONTAINS_CENSORED_WORD", exception.getCode());
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordsWithSpecialCharacters() {
        // Try to bypass with numbers and special characters
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is bu11shit")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is b*llshit")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is b_llsh!t")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("b-u-l-l-s-h-i-t")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordsWithSpacesBetween() {
        // Try to bypass with spaces between letters
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("b u l l s h i t")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("b  u  l  l  s  h  i  t")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForDutchCensoredWords() {
        // Test Dutch curse words from the list
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("Dit boek is echt debiel")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("Dit is debil")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordVariations() {
        // Test obfuscated variations (words with one character missing)
        // For example, "bullshit" with one character missing becomes "ullshit", "bllshit", etc.
        // These variations should be caught when obfuscation characters are present

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bull$hit")  // has obfuscation chars
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("b*llsht")  // combination of obfuscation
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForMultipleCensoredWordsInOneComment() {
        // Multiple bad words in same comment
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is bullshit and asshole")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordAsPartOfSentence() {
        // Bad word embedded in sentence
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("I think this book is utter bollocks")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("What a load of crap")
        );
    }

    @Test
    void validateReviewCommentShouldAllowCleanWordsContainingSubstringsOfBadWords() {
        // Words that contain substrings of bad words but aren't bad words themselves
        // These should be allowed
        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("ass is a donkey")
        );

        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("This book is good, not bad")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForMixedCaseAndObfuscation() {
        // Combination of mixed case and obfuscation
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This is B@LLSH!T")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("B#LL$H1T")
        );
    }

    @Test
    void validateReviewCommentShouldHandleVeryLongComments() {
        // Very long comment with a bad word hidden
        StringBuilder longComment = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longComment.append("This is a very long comment with a lot of text ");
        }
        longComment.append("but it contains bullshit");

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment(longComment.toString())
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordsWithPunctuation() {
        // Bad words with punctuation
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bullshit!")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("What the bullshit?")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("(bullshit)")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("[bullshit]")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordsAtStartAndEnd() {
        // Bad word at start
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("Bullshit review about this book")
        );

        // Bad word at end
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This review is bullshit")
        );
    }

    @Test
    void validateReviewCommentShouldTrimCommentBeforeValidating() {
        // Comment with leading/trailing spaces containing bad word
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("  bullshit  ")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForLeetspeakVariations() {
        // Common leetspeak substitutions
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bull5h1t")  // 5 for S, 1 for I
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bu11$h1t")  // 1 for I, $ for S
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("8u||5h17")  // 8 for B, || for LL, 7 for T
        );
    }

    @Test
    void validateReviewCommentShouldHandleUnicodeAndAccentedCharacters() {
        // Clean comments with accented characters should pass
        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("Très bon livre!")
        );

        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("Åt het graag")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCompoundCensoredWords() {
        // Test compound words formed by concatenating censored words
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bullshitfuck")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bullshitfuckpenis")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("fuckass")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bitchasshole")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCompoundWordsInSentence() {
        // Compound words embedded in sentences
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("This book is bullshitfuck terrible")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("What bullshitfuckpenis nonsense")
        );
    }

    @Test
    void validateReviewCommentShouldAllowCleanWordsNotComposedOfCensoredWords() {
        // Words that contain censored word substrings but aren't actually composed of them
        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("assignment")  // contains 'ass' but not composed of censored words
        );

        assertDoesNotThrow(() ->
            reviewModerationService.validateReviewComment("butterfinger")  // contains 'butt' but not composed of censored words
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCompoundWordsWithObfuscation() {
        // Compound words with special characters (obfuscation)
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bullshit$fuck")
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("fuck@ass#hole")
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordHiddenInLargerWord() {
        // Censored words hidden within larger words with padding
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("wateenkankerboek")  // "kanker" is hidden in the middle
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("Dit is watkankerwerk")  // "kanker" in the middle
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bookfuckinggreat")  // "fuck" hidden
        );
    }

    @Test
    void validateReviewCommentShouldThrowExceptionForCensoredWordAsSubstring() {
        // Censored words as substrings in larger words
        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("bitchfight")  // "bitch" is a substring
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("assholeism")  // "asshole" is a substring
        );

        assertThrows(ApiException.class, () ->
            reviewModerationService.validateReviewComment("fucked")  // "fuck" is a substring
        );
    }
}
