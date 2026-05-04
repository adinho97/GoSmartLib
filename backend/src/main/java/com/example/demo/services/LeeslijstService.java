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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeeslijstService {
    private final LeeslijstRepository leeslijstRepository;
    private final BookRepository bookRepository;
    private final KlasRepository klasRepository;
    private final AppUserRepository userRepository;
    private final SchoolRepository schoolRepository;

    public LeeslijstService(LeeslijstRepository leeslijstRepository,
                          BookRepository bookRepository,
                          KlasRepository klasRepository,
                          AppUserRepository userRepository,
                          SchoolRepository schoolRepository) {
        this.leeslijstRepository = leeslijstRepository;
        this.bookRepository = bookRepository;
        this.klasRepository = klasRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
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

        // Add books
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            List<Book> books = bookRepository.findAllById(request.getBookIds());
            leeslijst.getBooks().addAll(books);
        }

        // Add klassen
        if (request.getKlasIds() != null && !request.getKlasIds().isEmpty()) {
            List<Klas> klassen = klasRepository.findAllById(request.getKlasIds());
            leeslijst.getKlassen().addAll(klassen);
        }

        return leeslijstRepository.save(leeslijst);
    }

    public List<Leeslijst> getLeeslisten(Long schoolId) {
        return leeslijstRepository.findBySchool_Id(schoolId);
    }

    public List<Leeslijst> getLeeslistenForKlas(Long klasId) {
        return leeslijstRepository.findByKlas(klasId);
    }

    public List<Leeslijst> getLeeslistenForUser(String userSub) {
        AppUser user = userRepository.findBySub(userSub)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userSub));
        
        // Bibbeheerders should see all lists for their school for management purposes
        if ("bibbeheerder".equalsIgnoreCase(user.getRole()) && user.getSchool() != null) {
            return leeslijstRepository.findBySchool_Id(user.getSchool().getId());
        }

        // For other users (Teachers/Students), combine lists they created with lists for their class
        List<Leeslijst> createdByMe = leeslijstRepository.findByCreatedBy_Id(user.getId());
        
        if (user.getKlas() != null) {
            List<Leeslijst> forMyKlas = leeslijstRepository.findByKlas(user.getKlas().getId());
            
            // Use a Set to merge the lists and avoid duplicates (e.g., if a teacher created a list for their own class)
            Set<Leeslijst> combined = new HashSet<>(createdByMe);
            combined.addAll(forMyKlas);
            return new ArrayList<>(combined);
        }
        
        return createdByMe;
    }

    public Optional<Leeslijst> getLeeslijst(Long id) {
        return leeslijstRepository.findById(id);
    }

    public LeeslijstDTO getLeeslijstDTO(Long id) {
        Leeslijst leeslijst = leeslijstRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        LeeslijstDTO dto = new LeeslijstDTO(
            leeslijst.getId(),
            leeslijst.getTitel(),
            leeslijst.getDescription(),
            leeslijst.getSchool().getId(),
            resolveCreatedByName(leeslijst),
            leeslijst.getCreatedAt()
        );

        // Map books
        List<LeeslijstDTO.LeeslijstBookDTO> bookDTOs = leeslijst.getBooks().stream()
            .map(book -> new LeeslijstDTO.LeeslijstBookDTO(
                book.getId(),
                book.getTitel(),
                book.getAuteur(),
                book.getCover(),
                book.getGenre(),
                book.getPaginas(),
                book.getIsbn()
            ))
            .collect(Collectors.toList());
        dto.setBooks(bookDTOs);

        // Map klassen
        List<String> klasNames = leeslijst.getKlassen().stream()
            .map(Klas::getNaam)
            .collect(Collectors.toList());
        dto.setKlasNames(klasNames);

        List<Long> klasIds = leeslijst.getKlassen().stream()
            .map(Klas::getId)
            .collect(Collectors.toList());
        dto.setKlasIds(klasIds);

        if (leeslijst.getCreatedBy() != null) {
            dto.setCreatedBySub(leeslijst.getCreatedBy().getSub());
        }

        return dto;
    }

    public LeeslijstDTO convertToDTO(Leeslijst leeslijst) {
        LeeslijstDTO dto = new LeeslijstDTO(
            leeslijst.getId(),
            leeslijst.getTitel(),
            leeslijst.getDescription(),
            leeslijst.getSchool().getId(),
            resolveCreatedByName(leeslijst),
            leeslijst.getCreatedAt()
        );

        // Map books
        List<LeeslijstDTO.LeeslijstBookDTO> bookDTOs = leeslijst.getBooks().stream()
            .map(book -> new LeeslijstDTO.LeeslijstBookDTO(
                book.getId(),
                book.getTitel(),
                book.getAuteur(),
                book.getCover(),
                book.getGenre(),
                book.getPaginas(),
                book.getIsbn()
            ))
            .collect(Collectors.toList());
        dto.setBooks(bookDTOs);

        // Map klassen
        List<String> klasNames = leeslijst.getKlassen().stream()
            .map(Klas::getNaam)
            .collect(Collectors.toList());
        dto.setKlasNames(klasNames);

        List<Long> klasIds = leeslijst.getKlassen().stream()
            .map(Klas::getId)
            .collect(Collectors.toList());
        dto.setKlasIds(klasIds);

        if (leeslijst.getCreatedBy() != null) {
            dto.setCreatedBySub(leeslijst.getCreatedBy().getSub());
        }

        return dto;
    }

    public void deleteLeeslijst(Long id, String userSub, String userRole) {
        Leeslijst leeslijst = leeslijstRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        if (userRole == null || userRole.isBlank()) {
            throw new SecurityException("Missing role");
        }

        String normalizedRole = userRole.trim().toLowerCase();
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

    public Leeslijst updateLeeslijst(Long id, CreateLeeslijstRequest request, String userSub, String userRole) {
        Leeslijst leeslijst = leeslijstRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

        if (userRole == null || userRole.isBlank()) {
            throw new SecurityException("Missing role");
        }

        String normalizedRole = userRole.trim().toLowerCase();
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

        // Update books
        if (request.getBookIds() != null) {
            leeslijst.getBooks().clear();
            List<Book> books = bookRepository.findAllById(request.getBookIds());
            leeslijst.getBooks().addAll(books);
        }

        // Update klassen
        if (request.getKlasIds() != null) {
            leeslijst.getKlassen().clear();
            List<Klas> klassen = klasRepository.findAllById(request.getKlasIds());
            leeslijst.getKlassen().addAll(klassen);
        }

        return leeslijstRepository.save(leeslijst);
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
