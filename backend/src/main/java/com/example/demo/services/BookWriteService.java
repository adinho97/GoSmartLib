package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.exception.ApiException;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Objects;

@Service
@SuppressWarnings("null")
public class BookWriteService {

    private static final Logger logger = LoggerFactory.getLogger(BookWriteService.class);
    private static final SecureRandom GO_NUMBER_RANDOM = new SecureRandom();

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final SchoolService schoolService;

    public BookWriteService(BookRepository bookRepository, BookMapper bookMapper,
            SchoolService schoolService) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.schoolService = schoolService;
    }

    public BookDto create(BookDto bookDto) {
        logger.info("Creating book: titel={}, auteur={}, schoolId={}",
                bookDto.getTitel(), bookDto.getAuteur(), bookDto.getSchoolId());

        School school;
        try {
            school = schoolService.getByIdOrDefault(bookDto.getSchoolId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            logger.error("School not found or invalid: {}", bookDto.getSchoolId(), ex);
            throw new ApiException("Ongeldige school", HttpStatus.BAD_REQUEST, "INVALID_SCHOOL");
        }

        if (StringUtils.hasText(bookDto.getIsbn())) {
            Long schoolId = Objects.requireNonNull(school.getId(), "schoolId is required");
            if (bookRepository.findByIsbnAndSchool_Id(bookDto.getIsbn(), schoolId).isPresent()) {
                throw new ApiException("Boek met dit ISBN bestaat al voor deze school",
                        HttpStatus.CONFLICT, "ISBN_ALREADY_EXISTS");
            }
        }

        Book entity = bookMapper.toEntity(bookDto);
        entity.setId(null);
        entity.setGoNumber(null);
        entity.setSchool(school);
        assignGoNumberIfNeeded(entity);
        Book saved = bookRepository.save(entity);
        logger.info("Book saved with id: {}", saved.getId());

        return bookRepository.findById(saved.getId())
                .map(bookMapper::toDto)
                .orElse(bookMapper.toDto(saved));
    }

    public BookDto update(Long id, BookDto bookDto) {
        Book existing = bookRepository.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new ApiException("Boek niet gevonden",
                        HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        existing.setTitel(bookDto.getTitel());
        existing.setAuteur(bookDto.getAuteur());
        existing.setIsbn(bookDto.getIsbn());
        existing.setCover(bookDto.getCover());
        existing.setBeschrijving(bookDto.getBeschrijving());
        existing.setGenres(bookMapper.toEntity(bookDto).getGenres());
        existing.setUitgaveDatum(bookDto.getUitgaveDatum());
        existing.setPaginas(bookDto.getPaginas());
        existing.setTaal(bookDto.getTaal());
        existing.setUitgeverij(bookDto.getUitgeverij());
        if (bookDto.getLeesniveau() != null) {
            existing.setLeesniveau(bookDto.getLeesniveau().getLabel());
        }
        assignGoNumberIfNeeded(existing);

        if (bookDto.getSchoolId() != null) {
            School school = schoolService.getByIdOrDefault(bookDto.getSchoolId());
            existing.setSchool(school);
        }

        bookRepository.save(existing);

        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElseThrow(() -> new ApiException("Boek niet gevonden",
                        HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));
    }

    private void assignGoNumberIfNeeded(Book book) {
        if (book == null || StringUtils.hasText(book.getIsbn())) {
            return;
        }
        if (!StringUtils.hasText(book.getGoNumber())) {
            book.setGoNumber(generateUniqueGoNumber());
        }
    }

    private String generateUniqueGoNumber() {
        String goNumber;
        do {
            goNumber = "GO-" + String.format("%08d", GO_NUMBER_RANDOM.nextInt(100_000_000));
        } while (bookRepository.existsByGoNumber(goNumber));
        return goNumber;
    }
}
