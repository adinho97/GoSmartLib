package com.example.demo;

import com.example.demo.entities.UserPreference;
import com.example.demo.repositories.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@DisplayName("UserPreferenceRepository Tests")
class UserPreferenceRepositoryTest {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @BeforeEach
    void setUp() {
        userPreferenceRepository.deleteAll();
    }

    @Test
    @DisplayName("should find all preferences by user sub")
    void testFindByUserSub() {
        UserPreference pref1 = new UserPreference("user123", "key1", true);
        UserPreference pref2 = new UserPreference("user123", "key2", false);
        UserPreference pref3 = new UserPreference("user456", "key1", true);

        userPreferenceRepository.save(pref1);
        userPreferenceRepository.save(pref2);
        userPreferenceRepository.save(pref3);

        List<UserPreference> result = userPreferenceRepository.findByUserSub("user123");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getUserSub().equals("user123")));
    }

    @Test
    @DisplayName("should find specific preference by user sub and key")
    void testFindByUserSubAndPreferenceKey() {
        UserPreference pref = new UserPreference("user123", "myKey", true);
        userPreferenceRepository.save(pref);

        Optional<UserPreference> result = userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "myKey");

        assertTrue(result.isPresent());
        assertEquals("user123", result.get().getUserSub());
        assertEquals("myKey", result.get().getPreferenceKey());
        assertEquals(true, result.get().getPreferenceValue());
    }

    @Test
    @DisplayName("should return empty optional when preference not found")
    void testFindByUserSubAndPreferenceKeyNotFound() {
        Optional<UserPreference> result = userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "notFound");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should delete preference by user sub and key")
    void testDeleteByUserSubAndPreferenceKey() {
        UserPreference pref1 = new UserPreference("user123", "key1", true);
        UserPreference pref2 = new UserPreference("user123", "key2", false);
        userPreferenceRepository.save(pref1);
        userPreferenceRepository.save(pref2);

        userPreferenceRepository.deleteByUserSubAndPreferenceKey("user123", "key1");

        Optional<UserPreference> deleted = userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "key1");
        Optional<UserPreference> remaining = userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "key2");

        assertTrue(deleted.isEmpty());
        assertTrue(remaining.isPresent());
    }
}
