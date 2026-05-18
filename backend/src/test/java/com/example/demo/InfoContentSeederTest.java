package com.example.demo;

import com.example.demo.config.InfoContentSeeder;
import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.repositories.InfoContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InfoContentSeeder Tests")
@SuppressWarnings("null")
class InfoContentSeederTest {

    @Mock private InfoContentRepository repository;
    @InjectMocks private InfoContentSeeder seeder;

    @Test
    @DisplayName("seeds STAP and FEATURE globals when none exist")
    void seedsBothSectionsOnFirstRun() throws Exception {
        when(repository.existsBySchoolIsNullAndSectie(Sectie.STAP)).thenReturn(false);
        when(repository.existsBySchoolIsNullAndSectie(Sectie.FEATURE)).thenReturn(false);
        when(repository.save(any(InfoContent.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(repository, times(9)).save(captor.capture());

        List<InfoContent> all = captor.getAllValues();
        List<InfoContent> staps = all.stream().filter(i -> i.getSectie() == Sectie.STAP).toList();
        List<InfoContent> features = all.stream().filter(i -> i.getSectie() == Sectie.FEATURE).toList();

        assertEquals(5, staps.size(), "should seed 5 STAP defaults");
        assertEquals(4, features.size(), "should seed 4 FEATURE defaults");

        // All seeded rows are global (school is null) and have populated content.
        for (InfoContent ic : all) {
            assertNull(ic.getSchool(), "seeded defaults must be global");
            assertNotNull(ic.getInhoud());
            assertFalse(ic.getInhoud().isBlank());
        }

        // FEATURE entries carry a titel; STAP entries do not.
        for (InfoContent f : features) {
            assertNotNull(f.getTitel(), "FEATURE seed must include a title");
            assertFalse(f.getTitel().isBlank());
        }
        for (InfoContent s : staps) {
            assertNull(s.getTitel(), "STAP seed should not include a title");
        }

        // sortOrder is contiguous starting at 0 for each section.
        assertEquals(List.of(0, 1, 2, 3, 4),
            staps.stream().map(InfoContent::getSortOrder).toList());
        assertEquals(List.of(0, 1, 2, 3),
            features.stream().map(InfoContent::getSortOrder).toList());
    }

    @Test
    @DisplayName("skips STAP seeding when globals already exist")
    void skipsStapWhenAlreadyPresent() throws Exception {
        when(repository.existsBySchoolIsNullAndSectie(Sectie.STAP)).thenReturn(true);
        when(repository.existsBySchoolIsNullAndSectie(Sectie.FEATURE)).thenReturn(false);
        when(repository.save(any(InfoContent.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        assertTrue(captor.getAllValues().stream().noneMatch(i -> i.getSectie() == Sectie.STAP),
            "no STAP entries should be saved when already seeded");
        assertEquals(4, captor.getAllValues().stream()
            .filter(i -> i.getSectie() == Sectie.FEATURE).count());
    }

    @Test
    @DisplayName("does nothing when both sections are already seeded")
    void noopWhenFullySeeded() throws Exception {
        when(repository.existsBySchoolIsNullAndSectie(Sectie.STAP)).thenReturn(true);
        when(repository.existsBySchoolIsNullAndSectie(Sectie.FEATURE)).thenReturn(true);

        seeder.run();

        verify(repository, never()).save(any());
    }
}
