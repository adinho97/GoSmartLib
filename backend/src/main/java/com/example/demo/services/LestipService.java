package com.example.demo.services;

import com.example.demo.dto.LestipDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.entities.Book;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

@Service
@SuppressWarnings("null")
public class LestipService {

    private final BookRepository bookRepository;

    public LestipService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LestipDto getLestip(Long bookId, String scope, String userName) {
        Book localBook = requireBook(bookId);
        String normalizedUserName = normalizeUserName(userName);

        // Scope=school: only return the tip if it exists on the local record
        if ("school".equalsIgnoreCase(scope)) {
            return toLestipDto(localBook, normalizedUserName);
        }

        // Prioritize local tip
        if (hasLestip(localBook.getLestip())) {
            return toLestipDto(localBook, normalizedUserName);
        }

        // Fallback to global shared tip
        Book sourceBook = findSourceBookWithSharedLestip(localBook);
        LestipDto dto = toLestipDto(sourceBook, normalizedUserName);

        // If the tip is shared from another record/school, force read-only locally
        if (!Objects.equals(sourceBook.getId(), localBook.getId())) {
            dto.setMagVerwijderen(false);
        }
        return dto;
    }

    public LestipDto updateLestip(Long bookId, UpdateLestipRequest request, String userName) {
        String normalizedUserName = normalizeUserName(userName);
        Book book = requireBook(bookId);

        if (hasLestip(book.getLestip())) {
            throw new ApiException("Lestip bestaat al", HttpStatus.CONFLICT, "LESTIP_ALREADY_EXISTS");
        }

        String normalizedLestip = normalizeLestip(request.getLestip());
        if (normalizedLestip == null) {
            throw new ApiException("Lestip mag niet leeg zijn", HttpStatus.BAD_REQUEST, "LESTIP_EMPTY");
        }

        book.setLestip(normalizedLestip);
        book.setLestipAuteur(normalizedUserName);

        Book savedBook = bookRepository.save(book);
        return toLestipDto(savedBook, normalizedUserName);
    }

    public void deleteLestip(Long bookId, String userName) {
        String normalizedUserName = normalizeUserName(userName);
        Book book = requireBook(bookId);

        if (!hasLestip(book.getLestip())) {
            throw new ApiException("Geen lestip aanwezig", HttpStatus.NOT_FOUND, "LESTIP_NOT_FOUND");
        }

        String lestipAuteur = book.getLestipAuteur();
        boolean hasStoredAuteur = StringUtils.hasText(lestipAuteur);
        if (hasStoredAuteur
                && (normalizedUserName == null || !isSameUser(normalizedUserName, lestipAuteur))) {
            throw new ApiException("Je mag deze lestip niet verwijderen",
                    HttpStatus.FORBIDDEN, "LESTIP_NOT_AUTHOR");
        }

        book.setLestip(null);
        book.setLestipAuteur(null);
        bookRepository.save(book);
    }

    private Book requireBook(Long bookId) {
        return bookRepository.findById(Objects.requireNonNull(bookId, "bookId is required"))
                .orElseThrow(() -> new ApiException("Boek niet gevonden",
                        HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));
    }

    private Book findSourceBookWithSharedLestip(Book originalBook) {
        if (hasLestip(originalBook.getLestip())) {
            return originalBook;
        }

        if (StringUtils.hasText(originalBook.getIsbn())) {
            return bookRepository.findByIsbn(originalBook.getIsbn())
                    .filter(b -> hasLestip(b.getLestip()))
                    .orElse(originalBook);
        }

        if (StringUtils.hasText(originalBook.getGoNumber())) {
            return bookRepository.findByGoNumber(originalBook.getGoNumber())
                    .filter(b -> hasLestip(b.getLestip()))
                    .orElse(originalBook);
        }

        return originalBook;
    }

    private LestipDto toLestipDto(Book book, String currentUserName) {
        LestipDto dto = new LestipDto();
        String lestipText = book.getLestip();
        String lestipAuteur = book.getLestipAuteur();

        dto.setLestip(lestipText == null ? "" : lestipText);
        dto.setAuteurNaam(lestipAuteur == null ? "" : lestipAuteur);
        boolean magVerwijderen = hasLestip(lestipText)
                && (!StringUtils.hasText(lestipAuteur)
                        || (currentUserName != null && isSameUser(currentUserName, lestipAuteur)));
        dto.setMagVerwijderen(magVerwijderen);
        return dto;
    }

    private static String normalizeLestip(String lestip) {
        if (lestip == null) {
            return null;
        }
        String trimmedLestip = lestip.trim();
        return trimmedLestip.isEmpty() ? null : trimmedLestip;
    }

    private static String normalizeUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }
        String trimmedUserName = userName.trim();
        return trimmedUserName.isEmpty() ? null : trimmedUserName;
    }

    private static boolean hasLestip(String lestip) {
        return StringUtils.hasText(lestip);
    }

    private static boolean isSameUser(String firstUserName, String secondUserName) {
        String normalizedFirst = canonicalUserName(firstUserName);
        String normalizedSecond = canonicalUserName(secondUserName);
        return normalizedFirst != null && normalizedFirst.equals(normalizedSecond);
    }

    private static String canonicalUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }
        return userName
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
