package com.example.demo.services;

import com.example.demo.dto.LestipDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.entities.Book;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@SuppressWarnings("null")
public class LestipService {

    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "ppt", "pptx", "doc", "docx", "txt"));

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

        if (normalizedLestip.startsWith("{")) {
            try {
                var json = objectMapper.readTree(normalizedLestip);
                String fileName = json.path("fileName").asText(null);
                String fileData = json.path("fileData").asText(null);
                
                if (fileData != null && fileData.contains("base64,")) {
                    byte[] binaryData = Base64.getDecoder().decode(fileData.split("base64,")[1]);
                    validateAttachment(fileName, binaryData);
                }
            } catch (Exception e) {
                // Not valid JSON, treat as raw text
            }
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
            // Handle multiple books with same ISBN - find first one with lestip
            return bookRepository.findAllByIsbn(originalBook.getIsbn()).stream()
                    .filter(b -> hasLestip(b.getLestip()))
                    .findFirst()
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

        if (StringUtils.hasText(lestipText) && lestipText.trim().startsWith("{")) {
            try {
                var json = objectMapper.readTree(lestipText);
                dto.setLestip(json.path("text").asText(""));
                dto.setFileName(json.path("fileName").asText(null));
                dto.setFileContentType(json.path("fileContentType").asText(null));
                
                String b64 = json.path("fileData").asText(null);
                if (b64 != null) {
                    dto.setFileData(Base64.getDecoder().decode(b64.contains("base64,") ? b64.split("base64,")[1] : b64));
                }
            } catch (Exception e) {
                dto.setLestip(lestipText);
            }
        } else {
            dto.setLestip(lestipText == null ? "" : lestipText);
        }

        dto.setAuteurNaam(lestipAuteur == null ? "" : lestipAuteur);
        boolean magVerwijderen = hasLestip(lestipText)
                && (!StringUtils.hasText(lestipAuteur)
                        || (currentUserName != null && isSameUser(currentUserName, lestipAuteur)));
        dto.setMagVerwijderen(magVerwijderen);
        return dto;
    }

    private void validateAttachment(String fileName, byte[] fileData) {
        if (!StringUtils.hasText(fileName) || fileData == null || fileData.length == 0) return;

        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) extension = fileName.substring(i + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("Bestandstype niet toegestaan", HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE");
        }

        if (fileData.length > MAX_FILE_SIZE) {
            throw new ApiException("Bestand is te groot (max 25MB)", HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE");
        }
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
