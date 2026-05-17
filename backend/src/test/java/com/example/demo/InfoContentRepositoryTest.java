package com.example.demo;

import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.entities.InfoContentHidden;
import com.example.demo.entities.School;
import com.example.demo.repositories.InfoContentHiddenRepository;
import com.example.demo.repositories.InfoContentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // MySQL mode lets H2 parse the School entity's MySQL-specific column
    // definitions (e.g. DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)).
    "spring.datasource.url=jdbc:h2:mem:infodb;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@DisplayName("InfoContentRepository Tests")
class InfoContentRepositoryTest {

    @Autowired private InfoContentRepository infoContentRepository;
    @Autowired private InfoContentHiddenRepository infoContentHiddenRepository;
    @Autowired private EntityManager em;

    private School schoolA;
    private School schoolB;

    @BeforeEach
    void setUp() {
        infoContentHiddenRepository.deleteAll();
        infoContentRepository.deleteAll();
        em.createQuery("delete from School").executeUpdate();
        em.flush();

        schoolA = persistSchool("school-a", "https://school-a.example");
        schoolB = persistSchool("school-b", "https://school-b.example");
    }

    private School persistSchool(String subdomein, String url) {
        School s = new School();
        s.setSubdomein(subdomein);
        s.setSmartschoolUrl(url);
        em.persist(s);
        em.flush();
        return s;
    }

    private InfoContent persistItem(Sectie sectie, School school, String inhoud, int sortOrder) {
        InfoContent ic = new InfoContent();
        ic.setSectie(sectie);
        ic.setSchool(school);
        ic.setInhoud(inhoud);
        ic.setSortOrder(sortOrder);
        em.persist(ic);
        return ic;
    }

    private void persistHidden(School school, InfoContent item) {
        em.persist(new InfoContentHidden(school, item));
    }

    // ---- findGlobals ----

    @Test
    @DisplayName("findGlobals returns only school-null items for the given sectie, ordered")
    void findGlobalsReturnsGlobalsOnly() {
        persistItem(Sectie.TIP, null, "global tip A", 1);
        persistItem(Sectie.TIP, null, "global tip B", 0);
        persistItem(Sectie.TIP, schoolA, "school tip", 0);
        persistItem(Sectie.FAQ, null, "global faq", 0);
        em.flush();

        List<InfoContent> result = infoContentRepository.findGlobals(Sectie.TIP);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> i.getSchool() == null));
        // Ordered by sortOrder ASC: B (0), then A (1).
        assertEquals("global tip B", result.get(0).getInhoud());
        assertEquals("global tip A", result.get(1).getInhoud());
    }

    @Test
    @DisplayName("findGlobals returns empty when no globals exist for that sectie")
    void findGlobalsEmptyWhenNone() {
        persistItem(Sectie.TIP, schoolA, "school tip", 0);
        em.flush();

        assertTrue(infoContentRepository.findGlobals(Sectie.TIP).isEmpty());
    }

    // ---- findVisibleForSchool ----

    @Test
    @DisplayName("findVisibleForSchool returns school items plus globals that are not hidden")
    void findVisibleMergesSchoolAndGlobals() {
        persistItem(Sectie.TIP, null, "global 1", 1);
        persistItem(Sectie.TIP, null, "global 2", 3);
        persistItem(Sectie.TIP, schoolA, "school A tip", 0);
        persistItem(Sectie.TIP, schoolB, "school B tip", 2);
        em.flush();

        List<InfoContent> result = infoContentRepository.findVisibleForSchool(
            schoolA.getId(), Sectie.TIP);

        assertEquals(3, result.size());
        // Ordered by sortOrder ASC: school A (0), global 1 (1), global 2 (3).
        assertEquals("school A tip", result.get(0).getInhoud());
        assertEquals("global 1", result.get(1).getInhoud());
        assertEquals("global 2", result.get(2).getInhoud());
        // School B's item is not included.
        assertTrue(result.stream().noneMatch(i -> "school B tip".equals(i.getInhoud())));
    }

    @Test
    @DisplayName("findVisibleForSchool excludes globals that are hidden for this school")
    void findVisibleExcludesHiddenGlobals() {
        InfoContent visibleGlobal = persistItem(Sectie.FAQ, null, "visible global", 0);
        InfoContent hiddenGlobal = persistItem(Sectie.FAQ, null, "hidden global", 1);
        em.flush();

        persistHidden(schoolA, hiddenGlobal);
        em.flush();

        List<InfoContent> result = infoContentRepository.findVisibleForSchool(
            schoolA.getId(), Sectie.FAQ);

        assertEquals(1, result.size());
        assertEquals(visibleGlobal.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("findVisibleForSchool: a global hidden for school A is still visible for school B")
    void hiddenIsScopedPerSchool() {
        InfoContent global = persistItem(Sectie.TIP, null, "global", 0);
        em.flush();
        persistHidden(schoolA, global);
        em.flush();

        List<InfoContent> forA = infoContentRepository.findVisibleForSchool(
            schoolA.getId(), Sectie.TIP);
        List<InfoContent> forB = infoContentRepository.findVisibleForSchool(
            schoolB.getId(), Sectie.TIP);

        assertTrue(forA.isEmpty(), "school A hid it, so they should not see it");
        assertEquals(1, forB.size(), "school B did not hide it, so they still see it");
        assertEquals(global.getId(), forB.get(0).getId());
    }

    @Test
    @DisplayName("findVisibleForSchool filters by sectie")
    void findVisibleFiltersBySectie() {
        persistItem(Sectie.TIP, schoolA, "tip", 0);
        persistItem(Sectie.FAQ, schoolA, "faq", 0);
        em.flush();

        List<InfoContent> tips = infoContentRepository.findVisibleForSchool(
            schoolA.getId(), Sectie.TIP);
        List<InfoContent> faqs = infoContentRepository.findVisibleForSchool(
            schoolA.getId(), Sectie.FAQ);

        assertEquals(1, tips.size());
        assertEquals(Sectie.TIP, tips.get(0).getSectie());
        assertEquals(1, faqs.size());
        assertEquals(Sectie.FAQ, faqs.get(0).getSectie());
    }

    // ---- existsBySchoolIsNullAndSectie (used by the seeder) ----

    @Test
    @DisplayName("existsBySchoolIsNullAndSectie is true only when a global of that sectie exists")
    void existsBySchoolIsNullAndSectie() {
        assertFalse(infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.STAP));

        persistItem(Sectie.STAP, schoolA, "school-scoped step", 0);
        em.flush();
        assertFalse(infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.STAP),
            "a school-scoped item should not satisfy the 'global exists' check");

        persistItem(Sectie.STAP, null, "global step", 0);
        em.flush();
        assertTrue(infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.STAP));
        assertFalse(infoContentRepository.existsBySchoolIsNullAndSectie(Sectie.FEATURE));
    }
}
