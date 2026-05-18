package com.example.demo;

import com.example.demo.entities.UserDashboardConfig;
import com.example.demo.repositories.UserDashboardConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@DisplayName("UserDashboardConfigRepository Tests")
class UserDashboardConfigRepositoryTest {

    @Autowired
    private UserDashboardConfigRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("should persist custom tile config and retrieve it by userSub")
    void testSaveAndFindByUserSub() {
        String json = "{\"tiles\":[\"loans\",\"history\",\"recommendations\"],\"shortcuts\":[\"add-book\"]}";
        UserDashboardConfig config = new UserDashboardConfig("user-abc", json);

        repository.save(config);

        Optional<UserDashboardConfig> result = repository.findByUserSub("user-abc");
        assertTrue(result.isPresent());
        assertEquals("user-abc", result.get().getUserSub());
        assertEquals(json, result.get().getConfigJson());
        assertNotNull(result.get().getId());
    }

    @Test
    @DisplayName("should return empty optional when userSub has no saved config")
    void testFindByUserSubNotFound() {
        Optional<UserDashboardConfig> result = repository.findByUserSub("unknown-user");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should isolate configs across different users")
    void testFindByUserSubScopedToUser() {
        repository.save(new UserDashboardConfig("user-a", "{\"tiles\":[\"loans\"]}"));
        repository.save(new UserDashboardConfig("user-b", "{\"tiles\":[\"history\"]}"));

        Optional<UserDashboardConfig> a = repository.findByUserSub("user-a");
        Optional<UserDashboardConfig> b = repository.findByUserSub("user-b");

        assertTrue(a.isPresent());
        assertTrue(b.isPresent());
        assertEquals("{\"tiles\":[\"loans\"]}", a.get().getConfigJson());
        assertEquals("{\"tiles\":[\"history\"]}", b.get().getConfigJson());
    }

    @Test
    @DisplayName("should update configJson when saving an existing entity for the same user")
    void testUpdateExistingConfig() {
        UserDashboardConfig original = repository.save(
                new UserDashboardConfig("user-abc", "{\"tiles\":[\"loans\"]}"));
        Long originalId = original.getId();

        UserDashboardConfig fetched = repository.findByUserSub("user-abc").orElseThrow();
        fetched.setConfigJson("{\"tiles\":[\"loans\",\"history\"],\"shortcuts\":[\"add-book\"]}");
        repository.save(fetched);

        Optional<UserDashboardConfig> updated = repository.findByUserSub("user-abc");
        assertTrue(updated.isPresent());
        assertEquals(originalId, updated.get().getId());
        assertEquals("{\"tiles\":[\"loans\",\"history\"],\"shortcuts\":[\"add-book\"]}",
                updated.get().getConfigJson());
        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("should reject saving a second config row for the same userSub")
    void testUniqueConstraintOnUserSub() {
        repository.saveAndFlush(new UserDashboardConfig("user-abc", "{\"tiles\":[\"loans\"]}"));

        UserDashboardConfig duplicate = new UserDashboardConfig("user-abc", "{\"tiles\":[\"history\"]}");

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("should persist large json payloads up to the controller-enforced limit")
    void testPersistsLargeJsonPayload() {
        StringBuilder sb = new StringBuilder("{\"tiles\":\"");
        for (int i = 0; i < 7000; i++) {
            sb.append('x');
        }
        sb.append("\"}");
        String largeJson = sb.toString();

        repository.save(new UserDashboardConfig("user-large", largeJson));

        Optional<UserDashboardConfig> result = repository.findByUserSub("user-large");
        assertTrue(result.isPresent());
        assertEquals(largeJson.length(), result.get().getConfigJson().length());
    }
}
