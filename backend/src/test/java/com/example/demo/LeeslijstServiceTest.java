package com.example.demo;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Klas;
import com.example.demo.entities.Leeslijst;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.LeeslijstRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.services.LeeslijstService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeeslijstServiceTest {

    @Mock
    private LeeslijstRepository leeslijstRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private KlasRepository klasRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private LeeslijstService leeslijstService;

    private AppUser teacher;
    private School school;
    private Book book1;
    private Book book2;
    private Klas klas1;
    private Klas klas2;

    @BeforeEach
    void setUp() {
        school = new School();
        school.setId(5L);
        school.setNaam("Test School");

        teacher = new AppUser();
        teacher.setId(12L);
        teacher.setSub("teacher-sub");
        teacher.setRole("leerkracht");
        teacher.setSchool(school);

        book1 = new Book();
        book1.setId(1L);
        book1.setTitel("Dune");
        book1.setAuteur("Frank Herbert");

        book2 = new Book();
        book2.setId(2L);
        book2.setTitel("Foundation");
        book2.setAuteur("Isaac Asimov");

        klas1 = new Klas();
        klas1.setId(3L);
        klas1.setNaam("6A");

        klas2 = new Klas();
        klas2.setId(4L);
        klas2.setNaam("6B");
    }

    @Test
    void createLeeslijst_shouldPersistMergedBooksAndKlassen() {
        CreateLeeslijstRequest request = new CreateLeeslijstRequest();
        request.setTitel("Mijn leeslijst");
        request.setDescription("Beschrijving");
        request.setBookIds(List.of(1L, 2L));
        request.setKlasIds(List.of(3L, 4L));

        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));
        when(bookRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(book1, book2));
        when(klasRepository.findAllById(List.of(3L, 4L))).thenReturn(List.of(klas1, klas2));
        when(leeslijstRepository.save(any(Leeslijst.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Leeslijst result = leeslijstService.createLeeslijst(request, "teacher-sub", "Anna Berg");

        assertEquals("Mijn leeslijst", result.getTitel());
        assertEquals("Beschrijving", result.getDescription());
        assertEquals(school, result.getSchool());
        assertEquals(teacher, result.getCreatedBy());
        assertEquals("Anna Berg", result.getCreatedByName());
        assertEquals(2, result.getBooks().size());
        assertEquals(2, result.getKlassen().size());
        verify(leeslijstRepository).save(result);
    }

    @Test
    void createLeeslijst_shouldFailWhenUserHasNoSchool() {
        teacher.setSchool(null);
        CreateLeeslijstRequest request = new CreateLeeslijstRequest();
        request.setTitel("Lijst");

        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> leeslijstService.createLeeslijst(request, "teacher-sub", "Anna"));

        assertEquals("User has no school assigned", ex.getMessage());
        verify(leeslijstRepository, never()).save(any());
    }

    @Test
    void getLeeslistenForUser_shouldReturnCreatedAndClassListsForTeacher() {
        teacher.setKlas(klas1);
        Leeslijst created = new Leeslijst("Created", school, teacher);
        created.setId(1L);
        Leeslijst forClass = new Leeslijst("Class", school, teacher);
        forClass.setId(2L);

        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));
        when(leeslijstRepository.findByCreatedBy_Id(12L)).thenReturn(List.of(created));
        when(leeslijstRepository.findByKlas(3L)).thenReturn(List.of(forClass));

        List<Leeslijst> result = leeslijstService.getLeeslistenForUser("teacher-sub");

        assertEquals(2, result.size());
        assertTrue(result.contains(created));
        assertTrue(result.contains(forClass));
    }

    @Test
    void getLeeslistenForUser_shouldReturnSchoolListsForBibbeheerder() {
        teacher.setRole("bibbeheerder");
        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));
        Leeslijst list = new Leeslijst("School list", school, teacher);
        when(leeslijstRepository.findBySchool_Id(5L)).thenReturn(List.of(list));

        List<Leeslijst> result = leeslijstService.getLeeslistenForUser("teacher-sub");

        assertEquals(1, result.size());
        assertEquals("School list", result.get(0).getTitel());
    }

    @Test
    void getLeeslijstDTO_shouldMapBooksAndKlassen() {
        Leeslijst list = new Leeslijst("Lijst", school, teacher);
        list.setId(99L);
        list.setCreatedByName("Anna Berg");
        list.getBooks().add(book1);
        list.getBooks().add(book2);
        list.getKlassen().add(klas1);
        list.getKlassen().add(klas2);

        when(leeslijstRepository.findById(99L)).thenReturn(Optional.of(list));

        LeeslijstDTO dto = leeslijstService.getLeeslijstDTO(99L);

        assertEquals(99L, dto.getId());
        assertEquals("Lijst", dto.getTitel());
        assertEquals(5L, dto.getSchoolId());
        assertEquals("Anna Berg", dto.getCreatedByName());
        assertEquals(2, dto.getBooks().size());
        assertEquals(2, dto.getKlasIds().size());
        assertEquals(2, dto.getKlasNames().size());
    }

    @Test
    void updateLeeslijst_shouldReplaceBooksAndKlassenForAuthorizedTeacher() {
        Leeslijst list = new Leeslijst("Old", school, teacher);
        list.setId(50L);
        list.setCreatedBy(teacher);
        list.getBooks().add(book1);
        list.getKlassen().add(klas1);

        CreateLeeslijstRequest request = new CreateLeeslijstRequest();
        request.setTitel("New");
        request.setDescription("Updated");
        request.setBookIds(List.of(2L));
        request.setKlasIds(List.of(4L));

        when(leeslijstRepository.findById(50L)).thenReturn(Optional.of(list));
        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));
        when(bookRepository.findAllById(List.of(2L))).thenReturn(List.of(book2));
        when(klasRepository.findAllById(List.of(4L))).thenReturn(List.of(klas2));
        when(leeslijstRepository.save(any(Leeslijst.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Leeslijst result = leeslijstService.updateLeeslijst(50L, request, "teacher-sub");

        assertEquals("New", result.getTitel());
        assertEquals("Updated", result.getDescription());
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getKlassen().size());

        ArgumentCaptor<Leeslijst> captor = ArgumentCaptor.forClass(Leeslijst.class);
        verify(leeslijstRepository).save(captor.capture());
        assertEquals("New", captor.getValue().getTitel());
        assertEquals("Updated", captor.getValue().getDescription());
        assertTrue(captor.getValue().getBooks().contains(book2));
        assertTrue(captor.getValue().getKlassen().contains(klas2));
    }

    @Test
    void deleteLeeslijst_shouldDeleteWhenBibbeheerder() {
        AppUser bibbeheerder = new AppUser();
        bibbeheerder.setId(20L);
        bibbeheerder.setSub("bib-sub");
        bibbeheerder.setRole("bibbeheerder");
        bibbeheerder.setSchool(school);

        Leeslijst list = new Leeslijst("To delete", school, teacher);
        list.setId(60L);

        when(leeslijstRepository.findById(60L)).thenReturn(Optional.of(list));
        when(userRepository.findBySub("bib-sub")).thenReturn(Optional.of(bibbeheerder));

        leeslijstService.deleteLeeslijst(60L, "bib-sub");

        verify(leeslijstRepository).delete(list);
    }

    @Test
    void deleteLeeslijst_shouldRejectTeacherForAnotherUsersList() {
        AppUser otherUser = new AppUser();
        otherUser.setId(99L);
        otherUser.setSub("other-sub");
        otherUser.setRole("leerkracht");
        otherUser.setSchool(school);

        Leeslijst list = new Leeslijst("To delete", school, otherUser);
        list.setId(61L);

        when(leeslijstRepository.findById(61L)).thenReturn(Optional.of(list));
        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> leeslijstService.deleteLeeslijst(61L, "teacher-sub"));

        assertEquals("Teachers can only delete their own leeslijsten", ex.getMessage());
        verify(leeslijstRepository, never()).delete(any());
    }

    @Test
    void deleteLeeslijst_shouldRejectUnauthorizedRole() {
        AppUser student = new AppUser();
        student.setId(30L);
        student.setSub("student-sub");
        student.setRole("leerling");
        student.setSchool(school);

        Leeslijst list = new Leeslijst("To delete", school, teacher);
        list.setId(62L);

        when(leeslijstRepository.findById(62L)).thenReturn(Optional.of(list));
        when(userRepository.findBySub("student-sub")).thenReturn(Optional.of(student));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> leeslijstService.deleteLeeslijst(62L, "student-sub"));

        assertEquals("Not allowed to delete leeslijsten", ex.getMessage());
        verify(leeslijstRepository, never()).delete(any());
    }

    @Test
    void deleteLeeslijst_shouldThrowWhenUserNotFound() {
        Leeslijst list = new Leeslijst("To delete", school, teacher);
        list.setId(63L);

        when(leeslijstRepository.findById(63L)).thenReturn(Optional.of(list));
        when(userRepository.findBySub("unknown-sub")).thenReturn(Optional.empty());

        assertThrows(
                SecurityException.class,
                () -> leeslijstService.deleteLeeslijst(63L, "unknown-sub"));

        verify(leeslijstRepository, never()).delete(any());
    }

    @Test
    void createLeeslijst_shouldUseSubAsNameWhenUserNameBlank() {
        CreateLeeslijstRequest request = new CreateLeeslijstRequest();
        request.setTitel("Lijst");

        when(userRepository.findBySub("teacher-sub")).thenReturn(Optional.of(teacher));
        when(leeslijstRepository.save(any(Leeslijst.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Leeslijst result = leeslijstService.createLeeslijst(request, "teacher-sub", "  ");

        assertNotNull(result);
        assertEquals("teacher-sub", result.getCreatedByName());
    }
}
