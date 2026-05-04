package com.example.demo.services;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
            leeslijst.getCreatedBy().getSub(),
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

        return dto;
    }

    public LeeslijstDTO convertToDTO(Leeslijst leeslijst) {
        LeeslijstDTO dto = new LeeslijstDTO(
            leeslijst.getId(),
            leeslijst.getTitel(),
            leeslijst.getDescription(),
            leeslijst.getSchool().getId(),
            leeslijst.getCreatedBy().getSub(),
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

        return dto;
    }

    public void deleteLeeslijst(Long id) {
        leeslijstRepository.deleteById(id);
    }

    public Leeslijst updateLeeslijst(Long id, CreateLeeslijstRequest request) {
        Leeslijst leeslijst = leeslijstRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Leeslijst not found"));

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
}
