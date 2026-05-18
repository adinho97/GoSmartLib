package com.example.demo.services;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null")
public class LeeslijstService {
    private final LeeslijstRepository leeslijstRepository;
    private final BookRepository bookRepository;
    private final KlasRepository klasRepository;
    private final AppUserRepository userRepository;

    public LeeslijstService(LeeslijstRepository leeslijstRepository,
            BookRepository bookRepository,
            KlasRepository klasRepository,
            AppUserRepository userRepository) {
        this.leeslijstRepository = leeslijstRepository;
        this.bookRepository = bookRepository;
        this.klasRepository = klasRepository;
        this.userRepository = userRepository;
    }

    public Leeslijst createLeeslijst(CreateLeeslijstRequest request, String userSub) {
        return createLeeslijst(request, userSub, null);
    }

    public Leeslijst createLeeslijst(CreateLeeslijstRequest request, String userSub, String userName) {
        // Find user by their sub (Smartschool ID)
        AppUser user = userRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userSub));

        // Get user's school
        School school = user.getSchool();
        if (school == null) {
            throw new IllegalArgumentException("User has no school assigned");
        }

        Leeslijst leeslijst = new Leeslijst(request.getTitel(), school, user);
        leeslijst.setDescription(request.getDescription());
        leeslijst.setCreatedByName((userName == null || userName.isBlank()) ? userSub : userName);
        
        // Set global flag
        leeslijst.setIsGlobal(request.getIsGlobal() != null && request.getIsGlobal());

        // Add books
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            List<Long> bookIds = request.getBookIds().stream()
                    .filter(Objects::nonNull)
                    .map(id -> Objects.requireNonNull(id, "bookId is required"))
                    .toList();
            List<Book> books = bookRepository.findAllById(bookIds);
            leeslijst.getBooks().addAll(books);
        }

        // Add klassen
        if (request.getKlasIds() != null && !request.getKlasIds().isEmpty()) {
            List<Long> klasIds = request.getKlasIds().stream()
                    .filter(Objects::nonNull)
                    .map(id -> Objects.requireNonNull(id, "klasId is required"))
                    .toList();
            List<Klas> klassen = klasRepository.findAllById(klasIds);
            leeslijst.getKlassen().addAll(klassen);
        }

        // Add assigned users
        if (request.getAssignedUserSubs() != null && !request.getAssignedUserSubs().isEmpty()) {
            List<AppUser> assignedUsers = userRepository.findBySubIn(request.getAssignedUserSubs());
            leeslijst.getAssignedUsers().addAll(assignedUsers);
        }

        return leeslijstRepository.save(Objects.requireNonNull(leeslijst, "leeslijst is required"));
    }

    public List<Leeslijst> getLeeslisten(Long schoolId) {
        Long resolvedSchoolId = Objects.requireNonNull(schoolId, "schoolId is required");
        return leeslijstRepository.findBySchool_Id(resolvedSchoolId);
    }

    public List<Leeslijst> getLeeslistenForKlas(Long klasId) {
        Long resolvedKlasId = Objects.requireNonNull(klasId, "klasId is required");
        return leeslijstRepository.findByKlas(resolvedKlasId);
    }

    public List<Leeslijst> getLeeslistenForUser(String userSub) {
        AppUser user = userRepository.findBySub(userSub)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userSub));

        // Bibbeheerders should see:
        // 1. All lists for their school
        // 2. All global lists (for context)
        if ("bibbeheerder".equalsIgnoreCase(user.getRole()) && user.getSchool() != null) {
            Long schoolId = Objects.requireNonNull(user.getSchool().getId(), "School id is required");
            Set<Leeslijst> combined = new HashSet<>(leeslijstRepository.findBySchool_Id(schoolId));
            combined.addAll(leeslijstRepository.findByIsGlobalTrue());
            return new ArrayList<>(combined);
        }

        // For other users (Teachers/Students), combine:
        // 1. Lists they created
        // 2. Lists for their class
        // 3. Global lists
        // 4. Lists assigned to them specifically
        Long userId = Objects.requireNonNull(user.getId(), "userId is required");
        Set<Leeslijst> combined = new HashSet<>(leeslijstRepository.findByCreatedBy_Id(userId));

        // Get all global lists
        combined.addAll(leeslijstRepository.findByIsGlobalTrue());

        // Get lists assigned to this user
        combined.addAll(leeslijstRepository.findByAssignedUsers(userId));

        if (user.getKlas() != null) {
            Long klasId = Objects.requireNonNull(user.getKlas().getId(), "Klas id is required");
            combined.addAll(leeslijstRepository.findByKlas(klasId));
        }

        return new ArrayList<>(combined);
    }

    public Optional<Leeslijst> getLeeslijst(Long id) {
        Long resolvedId = Objects.requireNonNull(id, "leeslijstId is required");
        return leeslijstRepository.findById(resolvedId);
    }

    public LeeslijstDTO getLeeslijstDTO(Long id) {
        Long resolvedId = Objects.requireNonNull(id, "leeslijstId is required");
        Leeslijst leeslijst = leeslijstRepository.findById(resolvedId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        Long schoolId = Objects.requireNonNull(leeslijst.getSchool().getId(), "School id is required");
        LeeslijstDTO dto = new LeeslijstDTO(
            leeslijst.getId(),
            leeslijst.getTitel(),
            leeslijst.getDescription(),
            schoolId,
            resolveCreatedByName(leeslijst),
            leeslijst.getCreatedAt());

        dto.setIsGlobal(leeslijst.getIsGlobal());

        // Map books
        List<LeeslijstDTO.LeeslijstBookDTO> bookDTOs = leeslijst.getBooks().stream()
                .map(book -> new LeeslijstDTO.LeeslijstBookDTO(
                    Objects.requireNonNull(book.getId(), "bookId is required"),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getCover(),
                        book.getGenre(),
                        book.getPaginas(),
                        book.getIsbn()))
                .collect(Collectors.toList());
        dto.setBooks(bookDTOs);

        // Map klassen
        List<String> klasNames = leeslijst.getKlassen().stream()
                .map(Klas::getNaam)
                .collect(Collectors.toList());
        dto.setKlasNames(klasNames);

        List<Long> klasIds = leeslijst.getKlassen().stream()
            .map(klas -> Objects.requireNonNull(klas.getId(), "klasId is required"))
                .collect(Collectors.toList());
        dto.setKlasIds(klasIds);

        // Map assigned users
        List<LeeslijstDTO.UserDTO> userDTOs = leeslijst.getAssignedUsers().stream()
                .map(u -> new LeeslijstDTO.UserDTO(
                        u.getSub(),
                        u.getSub()))
                .collect(Collectors.toList());
        dto.setSharedWithUsers(userDTOs);

        if (leeslijst.getCreatedBy() != null) {
            dto.setCreatedBySub(leeslijst.getCreatedBy().getSub());
        }

        return dto;
    }

    public LeeslijstDTO convertToDTO(Leeslijst leeslijst) {
        Long schoolId = Objects.requireNonNull(leeslijst.getSchool().getId(), "School id is required");
        LeeslijstDTO dto = new LeeslijstDTO(
            leeslijst.getId(),
            leeslijst.getTitel(),
            leeslijst.getDescription(),
            schoolId,
            resolveCreatedByName(leeslijst),
            leeslijst.getCreatedAt());

        dto.setIsGlobal(leeslijst.getIsGlobal());

        // Map books
        List<LeeslijstDTO.LeeslijstBookDTO> bookDTOs = leeslijst.getBooks().stream()
                .map(book -> new LeeslijstDTO.LeeslijstBookDTO(
                    Objects.requireNonNull(book.getId(), "bookId is required"),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getCover(),
                        book.getGenre(),
                        book.getPaginas(),
                        book.getIsbn()))
                .collect(Collectors.toList());
        dto.setBooks(bookDTOs);

        // Map klassen
        List<String> klasNames = leeslijst.getKlassen().stream()
                .map(Klas::getNaam)
                .collect(Collectors.toList());
        dto.setKlasNames(klasNames);

        List<Long> klasIds = leeslijst.getKlassen().stream()
            .map(klas -> Objects.requireNonNull(klas.getId(), "klasId is required"))
                .collect(Collectors.toList());
        dto.setKlasIds(klasIds);

        // Map assigned users
        List<LeeslijstDTO.UserDTO> userDTOs = leeslijst.getAssignedUsers().stream()
                .map(u -> new LeeslijstDTO.UserDTO(
                        u.getSub(),
                        u.getSub()))
                .collect(Collectors.toList());
        dto.setSharedWithUsers(userDTOs);

        if (leeslijst.getCreatedBy() != null) {
            dto.setCreatedBySub(leeslijst.getCreatedBy().getSub());
        }

        return dto;
    }

    public void deleteLeeslijst(Long id, String userSub) {
        Long resolvedId = Objects.requireNonNull(id, "leeslijstId is required");
        Leeslijst leeslijst = leeslijstRepository.findById(resolvedId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        AppUser user = userRepository.findBySub(userSub)
                .orElseThrow(() -> new SecurityException("User not found"));
        String normalizedRole = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();

        if ("leerkracht".equals(normalizedRole)) {
            if (leeslijst.getCreatedBy() == null || leeslijst.getCreatedBy().getSub() == null
                    || !leeslijst.getCreatedBy().getSub().equals(userSub)) {
                throw new SecurityException("Teachers can only delete their own leeslijsten");
            }
        } else if (!"bibbeheerder".equals(normalizedRole)) {
            throw new SecurityException("Not allowed to delete leeslijsten");
        }

        leeslijstRepository.delete(leeslijst);
    }

    public Leeslijst updateLeeslijst(Long id, CreateLeeslijstRequest request, String userSub) {
        Long resolvedId = Objects.requireNonNull(id, "leeslijstId is required");
        Leeslijst leeslijst = leeslijstRepository.findById(resolvedId)
                .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        AppUser user = userRepository.findBySub(userSub)
                .orElseThrow(() -> new SecurityException("User not found"));
        String normalizedRole = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();

        if ("leerkracht".equals(normalizedRole)) {
            if (leeslijst.getCreatedBy() == null || leeslijst.getCreatedBy().getSub() == null
                    || !leeslijst.getCreatedBy().getSub().equals(userSub)) {
                throw new SecurityException("Teachers can only edit their own leeslijsten");
            }
        } else if (!"bibbeheerder".equals(normalizedRole)) {
            throw new SecurityException("Not allowed to edit leeslijsten");
        }

        leeslijst.setTitel(request.getTitel());
        leeslijst.setDescription(request.getDescription());
        leeslijst.setIsGlobal(request.getIsGlobal() != null && request.getIsGlobal());

        // Update books
        if (request.getBookIds() != null) {
            leeslijst.getBooks().clear();
            List<Long> bookIds = request.getBookIds().stream()
                    .filter(Objects::nonNull)
                    .map(bookId -> Objects.requireNonNull(bookId, "bookId is required"))
                    .toList();
            List<Book> books = bookRepository.findAllById(bookIds);
            leeslijst.getBooks().addAll(books);
        }

        // Update klassen
        if (request.getKlasIds() != null) {
            leeslijst.getKlassen().clear();
            List<Long> klasIds = request.getKlasIds().stream()
                    .filter(Objects::nonNull)
                    .map(klasId -> Objects.requireNonNull(klasId, "klasId is required"))
                    .toList();
            List<Klas> klassen = klasRepository.findAllById(klasIds);
            leeslijst.getKlassen().addAll(klassen);
        }

        // Update assigned users
        if (request.getAssignedUserSubs() != null) {
            leeslijst.getAssignedUsers().clear();
            List<AppUser> assignedUsers = userRepository.findBySubIn(request.getAssignedUserSubs());
            leeslijst.getAssignedUsers().addAll(assignedUsers);
        }

        return leeslijstRepository.save(Objects.requireNonNull(leeslijst, "leeslijst is required"));
    }

    private String resolveCreatedByName(Leeslijst leeslijst) {
        if (leeslijst.getCreatedByName() != null && !leeslijst.getCreatedByName().isBlank()) {
            return leeslijst.getCreatedByName();
        }

        if (leeslijst.getCreatedBy() != null && leeslijst.getCreatedBy().getSub() != null
                && !leeslijst.getCreatedBy().getSub().isBlank()) {
            return leeslijst.getCreatedBy().getSub();
        }

        return "Onbekende gebruiker";
    }
}
