package com.example.demo.services;

import com.example.demo.dto.LestipDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.entities.Book;
import com.example.demo.entities.LestipAttachment;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
@Transactional
public class LestipService {

    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "ppt", "pptx", "doc", "docx", "txt"));

    public LestipService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
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

    @Transactional
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

                String text = json.path("text").asText("").trim();
                if (text.isEmpty()) {
                    throw new ApiException("Een beschrijving is verplicht bij een lestip.", HttpStatus.BAD_REQUEST, "LESTIP_TEXT_REQUIRED");
                }

                // Clear existing attachments to replace with new set
                book.getLestipAttachments().clear();

                JsonNode attachmentsNode = json.path("attachments");
                if (attachmentsNode.isArray() && !attachmentsNode.isEmpty()) {
                    for (JsonNode attachmentNode : attachmentsNode) {
                        processAttachmentNode(book, attachmentNode);
                    }
                    // Remove binary data from JSON to keep text column small
                    for (JsonNode attachmentNode : attachmentsNode) {
                        if (attachmentNode instanceof ObjectNode) {
                            ((ObjectNode) attachmentNode).remove("fileData");
                        }
                    }
                    normalizedLestip = json.toString();
                } else {
                    // Fallback to legacy single file logic
                    String fileName = json.path("fileName").asText(null);
                    String fileData = json.path("fileData").asText(null);
                    String contentType = json.path("fileContentType").asText("application/octet-stream");

                    if (fileData != null && fileData.contains("base64,")) {
                        String pureBase64 = fileData.substring(fileData.indexOf(",") + 1);
                        byte[] binaryData = Base64.getDecoder().decode(pureBase64);
                        validateAttachment(fileName, binaryData);

                        LestipAttachment attachment = new LestipAttachment();
                        attachment.setBook(book);
                        attachment.setFileName(fileName);
                        attachment.setContentType(contentType);
                        attachment.setFileData(binaryData);
                        book.getLestipAttachments().add(attachment);

                        ((ObjectNode) json).remove("fileData");
                        normalizedLestip = json.toString();
                    }
                }
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                // Not valid JSON, treat as raw text
            }
        }

        book.setLestip(normalizedLestip);
        book.setLestipAuteur(normalizedUserName);

        Book savedBook = bookRepository.save(book);
        return toLestipDto(savedBook, normalizedUserName);
    }

    private void processAttachmentNode(Book book, JsonNode node) {
        String fileName = node.path("fileName").asText(null);
        String fileData = node.path("fileData").asText(null);
        String contentType = node.path("contentType").asText("application/octet-stream");

        if (fileData != null && fileData.contains("base64,")) {
            String pureBase64 = fileData.substring(fileData.indexOf(",") + 1);
            byte[] binaryData = Base64.getDecoder().decode(pureBase64);
            validateAttachment(fileName, binaryData);

            LestipAttachment attachment = new LestipAttachment();
            attachment.setBook(book);
            attachment.setFileName(fileName);
            attachment.setContentType(contentType);
            attachment.setFileData(binaryData);
            book.getLestipAttachments().add(attachment);
        }
    }

    @Transactional
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
        if (book.getLestipAttachments() != null) {
            book.getLestipAttachments().clear();
        }
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
                String text = json.path("text").asText("");
                dto.setLestip(text);

                if (book.getLestipAttachments() != null && !book.getLestipAttachments().isEmpty()) {
                    // Vul de lijst met alle bijlagen voor de frontend
                    List<LestipDto.AttachmentDto> attachmentDtos = book.getLestipAttachments().stream()
                            .map(att -> {
                                LestipDto.AttachmentDto a = new LestipDto.AttachmentDto();
                                a.setFileName(att.getFileName());
                                a.setContentType(att.getContentType());
                                // Base64 encoding voor transfer naar frontend
                                a.setFileData(Base64.getEncoder().encodeToString(att.getFileData()));
                                return a;
                            })
                            .collect(Collectors.toList());
                    dto.setAttachments(attachmentDtos);
                    
                    // Behoud compatibiliteit voor de velden op het hoofdniveau van de DTO
                    LestipDto.AttachmentDto first = attachmentDtos.get(0);
                    dto.setFileName(first.getFileName());
                    dto.setFileContentType(first.getContentType());
                } else {
                    dto.setFileName(json.path("fileName").asText(null));
                    dto.setFileContentType(json.path("fileContentType").asText(null));
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
