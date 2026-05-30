package com.example.demo.services;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.dto.ExtensionRequestTicket;
import com.example.demo.dto.LoanExtensionRequestDto;
import com.example.demo.entities.LoanExtensionRequest;
import com.example.demo.dto.ReturnLoanRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanExtensionRequestRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.dto.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    private final LoanRepository loanRepo;
    private final BookCopyRepository copyRepo;
    private final BookAvailabilityNotificationService bookAvailabilityNotificationService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final AuthService authService;
    private final SmartschoolProperties smartschoolProperties;
    private final AppUserRepository appUserRepository;
    private final DisplayNameResolver displayNameResolver; // Injected DisplayNameResolver
    private final LoanExtensionRequestRepository loanExtensionRequestRepository;

    public LoanService(LoanRepository loanRepo, BookCopyRepository copyRepo,
            BookAvailabilityNotificationService bookAvailabilityNotificationService,
            SmartschoolMessageService smartschoolMessageService,
            AuthService authService,
            SmartschoolProperties smartschoolProperties,
            AppUserRepository appUserRepository,
            DisplayNameResolver displayNameResolver,
            LoanExtensionRequestRepository loanExtensionRequestRepository) {
        this.loanRepo = loanRepo;
        this.displayNameResolver = displayNameResolver;
        this.copyRepo = copyRepo;
        this.bookAvailabilityNotificationService = bookAvailabilityNotificationService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.loanExtensionRequestRepository = loanExtensionRequestRepository;
        this.authService = authService;
        this.smartschoolProperties = smartschoolProperties;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request, String lenderSub) {
        return createLoan(request, lenderSub, true);
    }

    @Transactional
    public List<LoanDto> createLoans(List<CreateLoanRequest> requests, String lenderSub) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        String userSub = requests.get(0).getUserSub();
        if (requests.stream()
                .anyMatch(request -> request.getUserSub() == null || !request.getUserSub().equals(userSub))) {
            throw new IllegalArgumentException("All loans in one batch must belong to the same user");
        }

        LocalDate dueDate = requests.get(0).getDueDate();
        if (requests.stream()
                .anyMatch(request -> request.getDueDate() == null || !request.getDueDate().equals(dueDate))) {
            throw new IllegalArgumentException("All loans in one batch must have the same due date");
        }

        List<LoanDto> createdLoans = new ArrayList<>();
        for (CreateLoanRequest request : requests) {
            createdLoans.add(createLoan(request, lenderSub, false));
        }

        sendCombinedLoanConfirmationForDtos(userSub, createdLoans);
        return createdLoans;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request, String lenderSub, boolean sendMessage) {
        logger.info("Creating loan: bookId={}, copyId={}, userSub={}, dueDate={}, sendMessage={}",
                request.getBookId(), request.getCopyId(), request.getUserSub(), request.getDueDate(), sendMessage);

        if (request.getBookId() == null) {
            logger.error("Invalid loan request: bookId is null");
            throw new IllegalArgumentException("Book ID is required");
        }
        if (request.getUserSub() == null || request.getUserSub().isBlank()) {
            logger.error("Invalid loan request: userSub is empty");
            throw new IllegalArgumentException("User sub is required");
        }
        if (request.getDueDate() == null) {
            logger.error("Invalid loan request: dueDate is null");
            throw new IllegalArgumentException("Due date is required");
        }

        List<BookCopy> lendableCopies = copyRepo.findByBook_Id(request.getBookId())
                .stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                        || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
                .sorted((a, b) -> {
                    // Prefer a copy in good state before lending out a damaged one.
                    int rankA = a.getStatus() == BookCopy.CopyStatus.AVAILABLE ? 0 : 1;
                    int rankB = b.getStatus() == BookCopy.CopyStatus.AVAILABLE ? 0 : 1;
                    return Integer.compare(rankA, rankB);
                })
                .collect(Collectors.toList());

        if (lendableCopies.isEmpty()) {
            logger.warn("No available copies for bookId={}", request.getBookId());
            throw new IllegalStateException("Geen beschikbare exemplaren");
        }

        Long bookSchoolId = lendableCopies.get(0).getBook().getSchool() != null
                ? lendableCopies.get(0).getBook().getSchool().getId()
                : null;

        if (lenderSub != null && !lenderSub.isBlank()) {
            AppUser lender = appUserRepository.findBySub(lenderSub.trim()).orElse(null);
            if (lender != null && lender.getSchool() != null) {
                Long lenderSchoolId = lender.getSchool().getId();
                if (!lenderSchoolId.equals(bookSchoolId)) {
                    logger.warn("School mismatch for loan: book schoolId={}, lender schoolId={}, lenderSub={}",
                            bookSchoolId, lenderSchoolId, lenderSub);
                    throw new IllegalArgumentException("Je kan enkel boeken van je eigen school uitlenen");
                }
            }
        }

        AppUser borrower = appUserRepository.findBySub(request.getUserSub()).orElse(null);
        if (borrower != null && "leerling".equalsIgnoreCase(borrower.getRole())) {
            Long studentSchoolId = borrower.getSchool() != null ? borrower.getSchool().getId() : null;
            if (studentSchoolId != null && !studentSchoolId.equals(bookSchoolId)) {
                logger.warn("School mismatch for loan: book schoolId={}, student schoolId={}, userSub={}",
                        bookSchoolId, studentSchoolId, request.getUserSub());
                throw new IllegalArgumentException("Leerling kan enkel boeken van de eigen school ontlenen");
            }
        }

        BookCopy copy;
        if (request.getCopyId() != null) {
            copy = lendableCopies.stream()
                    .filter(c -> c.getId().equals(request.getCopyId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Gekozen exemplaar is niet beschikbaar"));
        } else {
            copy = lendableCopies.get(0);
        }
        copy.setStatus(BookCopy.CopyStatus.LOANED);
        copyRepo.save(copy);
        logger.info("Marked copy {} as LOANED", copy.getId());

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setUserSub(request.getUserSub());
        loan.setLoanedAt(LocalDate.now());
        loan.setDueDate(request.getDueDate());
        loan.setLoanedCondition(copy.getCondition());

        Loan savedLoan = loanRepo.save(loan);
        logger.info("Loan created: id={}, bookId={}, userSub={}", savedLoan.getId(), request.getBookId(),
                request.getUserSub());

        if (sendMessage) {
            sendLoanConfirmationMessage(savedLoan);
        }

        return toDto(savedLoan);
    }

    private void sendLoanConfirmationMessage(Loan loan) {
        try {
            authService.getUserInfoBySub(loan.getUserSub())
                    .flatMap(userInfo -> {
                        SmartschoolMessageRequest req = new SmartschoolMessageRequest();
                        String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                                : smartschoolProperties.getApiBaseUrl();

                        req.setPlatformUrl(platform);
                        req.setSubject("Bevestiging: uitlening bibliotheekboek");
                        req.setBody(buildSingleLoanHtml(
                                userInfo.getName() != null ? userInfo.getName() : "Lezer",
                                loan.getCopy().getBook().getTitel(),
                                loan.getDueDate() != null ? loan.getDueDate().toString() : "onbekend"));

                        return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                    })
                    .doOnSuccess(res -> logger.info("Sent loan confirmation to {} for {}", loan.getUserSub(),
                            loan.getCopy().getBook().getTitel()))
                    .doOnError(err -> logger.error("Failed to send loan confirmation for {}: {}", loan.getUserSub(),
                            err.getMessage()))
                    .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                    .subscribe();
        } catch (RuntimeException ex) {
            logger.warn("Failed to start Smartschool confirmation pipeline ({}): {}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    public void sendCombinedLoanConfirmation(String userSub, List<Loan> loans) {
        if (loans == null || loans.isEmpty()) {
            logger.warn("No loans to send combined confirmation for user: {}", userSub);
            return;
        }

        if (loans.size() == 1) {
            sendLoanConfirmationMessage(loans.get(0));
            return;
        }

        try {
            authService.getUserInfoBySub(userSub)
                    .flatMap(userInfo -> {
                        SmartschoolMessageRequest req = new SmartschoolMessageRequest();
                        String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                                : smartschoolProperties.getApiBaseUrl();

                        req.setPlatformUrl(platform);
                        req.setSubject(String.format("Bevestiging: uitlening %d boeken", loans.size()));

                        String name = userInfo.getName() != null ? userInfo.getName() : "Lezer";
                        LocalDate dueDate = loans.get(0).getDueDate();
                        String dueDateStr = dueDate != null ? dueDate.toString() : "onbekend";
                        List<String> titles = loans.stream()
                                .map(l -> l.getCopy().getBook().getTitel())
                                .collect(Collectors.toList());

                        req.setBody(buildCombinedLoanHtml(name, titles, dueDateStr));

                        return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                    })
                    .doOnSuccess(res -> logger.info("Sent combined loan confirmation to {} for {} books", userSub,
                            loans.size()))
                    .doOnError(err -> logger.error("Failed to send combined loan confirmation for {}: {}", userSub,
                            err.getMessage()))
                    .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                    .subscribe();
        } catch (RuntimeException ex) {
            logger.warn("Failed to start combined Smartschool confirmation pipeline ({}): {}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    public void sendCombinedLoanConfirmationForDtos(String userSub, List<LoanDto> loans) {
        if (loans == null || loans.isEmpty()) {
            logger.warn("No loans to send combined confirmation for user: {}", userSub);
            return;
        }

        if (loans.size() == 1) {
            LoanDto loan = loans.get(0);
            String dueDateStr = loan.getDueDate() != null ? loan.getDueDate().toString() : "onbekend";
            sendSingleLoanConfirmationByDto(userSub, loan.getBookTitel(), dueDateStr);
            return;
        }

        try {
            authService.getUserInfoBySub(userSub)
                    .flatMap(userInfo -> {
                        SmartschoolMessageRequest req = new SmartschoolMessageRequest();
                        String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                                : smartschoolProperties.getApiBaseUrl();

                        req.setPlatformUrl(platform);
                        req.setSubject(String.format("Bevestiging: uitlening %d boeken", loans.size()));

                        String name = userInfo.getName() != null ? userInfo.getName() : "Lezer";
                        LocalDate dueDate = loans.get(0).getDueDate();
                        String dueDateStr = dueDate != null ? dueDate.toString() : "onbekend";
                        List<String> titles = loans.stream()
                                .map(LoanDto::getBookTitel)
                                .collect(Collectors.toList());

                        req.setBody(buildCombinedLoanHtml(name, titles, dueDateStr));

                        return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                    })
                    .doOnSuccess(res -> logger.info("Sent combined loan confirmation to {} for {} books", userSub,
                            loans.size()))
                    .doOnError(err -> logger.error("Failed to send combined loan confirmation for {}: {}", userSub,
                            err.getMessage()))
                    .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                    .subscribe();
        } catch (RuntimeException ex) {
            logger.warn("Failed to start combined Smartschool confirmation pipeline ({}): {}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    public void sendCombinedLoanConfirmationByLoans(List<Loan> loans) {
        if (loans == null || loans.isEmpty()) {
            logger.warn("No loans to send combined confirmation for");
            return;
        }

        String userSub = loans.get(0).getUserSub();
        if (!loans.stream().allMatch(l -> l.getUserSub().equals(userSub))) {
            logger.error("Cannot send combined confirmation for loans belonging to different users");
            return;
        }

        sendCombinedLoanConfirmation(userSub, loans);
    }

    private void sendSingleLoanConfirmationByDto(String userSub, String title, String dueDateStr) {
        try {
            authService.getUserInfoBySub(userSub)
                    .flatMap(userInfo -> {
                        SmartschoolMessageRequest req = new SmartschoolMessageRequest();
                        String platform = (userInfo.getPlatform() != null) ? userInfo.getPlatform()
                                : smartschoolProperties.getApiBaseUrl();

                        req.setPlatformUrl(platform);
                        req.setSubject("Bevestiging: uitlening bibliotheekboek");
                        req.setBody(buildSingleLoanHtml(
                                userInfo.getName() != null ? userInfo.getName() : "Lezer",
                                title,
                                dueDateStr));

                        return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                    })
                    .doOnSuccess(res -> logger.info("Sent single loan confirmation to {} for {}", userSub, title))
                    .doOnError(err -> logger.error("Failed to send single loan confirmation for {}: {}", userSub,
                            err.getMessage()))
                    .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                    .subscribe();
        } catch (RuntimeException ex) {
            logger.warn("Failed to start Smartschool confirmation pipeline ({}): {}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    // ── HTML builders ──────────────────────────────────────────────────────────

    private String buildSingleLoanHtml(String name, String title, String dueDateStr) {
        return String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                          <div style="background-color: #1a3a5c; padding: 24px 32px;">
                            <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">
                               Bibliotheek — Uitleenbevestiging
                            </h1>
                          </div>
                          <div style="padding: 28px 32px;">
                            <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                            <p style="margin: 0 0 24px; font-size: 15px; color: #333;">Hieronder vindt u het boek dat u hebt geleend.</p>
                            <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Arial, sans-serif;">
                              <thead>
                                <tr style="background-color: #1a3a5c; color: #fff;">
                                  <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                                </tr>
                              </thead>
                              <tbody>
                                <tr style="background-color: #f9f9f9;">
                                  <td style="padding: 8px 12px; font-size: 14px; color: #222;">%s</td>
                                </tr>
                              </tbody>
                            </table>
                            <div style="background-color: #fff8e1; border-left: 4px solid #f0a500; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                              <p style="margin: 0; font-size: 14px; color: #7a5c00;"><strong>Teruggavedatum:</strong> %s</p>
                              <p style="margin: 6px 0 0; font-size: 13px; color: #9a7a20;">Gelieve het boek op deze datum terug te brengen.</p>
                            </div>
                            <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
                          </div>
                          <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                            <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                          </div>
                        </div>
                        """,
                escapeHtml(name), escapeHtml(title), dueDateStr);
    }

    private String buildCombinedLoanHtml(String name, List<String> titles, String dueDateStr) {
        StringBuilder bookRows = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            String rowColor = (i % 2 == 0) ? "#f9f9f9" : "#ffffff";
            bookRows.append(String.format(
                    "<tr style=\"background-color:%s;\">" +
                            "  <td style=\"padding:8px 12px; color:#555; font-size:14px; width:40px;\">%d</td>" +
                            "  <td style=\"padding:8px 12px; font-size:14px; color:#222;\">%s</td>" +
                            "</tr>",
                    rowColor, i + 1, escapeHtml(titles.get(i))));
        }

        return String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                          <div style="background-color: #1a3a5c; padding: 24px 32px;">
                            <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">
                               Bibliotheek — Uitleenbevestiging
                            </h1>
                          </div>
                          <div style="padding: 28px 32px;">
                            <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                            <p style="margin: 0 0 24px; font-size: 15px; color: #333;">Hieronder vindt u een overzicht van de <strong>%d boeken</strong> die u hebt geleend.</p>
                            <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Arial, sans-serif;">
                              <thead>
                                <tr style="background-color: #1a3a5c; color: #fff;">
                                  <th style="padding: 10px 12px; text-align: left; font-size: 13px; width: 40px;">#</th>
                                  <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                                </tr>
                              </thead>
                              <tbody>%s</tbody>
                            </table>
                            <div style="background-color: #fff8e1; border-left: 4px solid #f0a500; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                              <p style="margin: 0; font-size: 14px; color: #7a5c00;"><strong>Teruggavedatum:</strong> %s</p>
                              <p style="margin: 6px 0 0; font-size: 13px; color: #9a7a20;">Gelieve alle boeken op deze datum terug te brengen.</p>
                            </div>
                            <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
                          </div>
                          <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                            <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
                          </div>
                        </div>
                        """,
                escapeHtml(name), titles.size(), bookRows.toString(), dueDateStr);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Transactional
    public void createExtensionRequest(Long loanId, ExtensionRequestTicket ticket) {
        if (loanId == null || ticket == null || ticket.getSenderSub() == null || ticket.getSenderSub().isBlank()) {
            throw new ApiException("Ongeldige aanvraag data", HttpStatus.BAD_REQUEST, "INVALID_TICKET");
        }

        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Lening niet gevonden"));

        if (loan.getReturnedAt() != null) {
            throw new ApiException("Kan geen verlenging aanvragen voor een ingeleverd boek", HttpStatus.BAD_REQUEST,
                    "LOAN_ALREADY_RETURNED");
        }

        if (loanExtensionRequestRepository.existsByLoan_IdAndStatus(loanId,
                LoanExtensionRequest.RequestStatus.PENDING)) {
            throw new ApiException("Er is al een lopende aanvraag voor dit boek", HttpStatus.CONFLICT,
                    "PENDING_REQUEST_EXISTS");
        }

        LoanExtensionRequest extensionRequest = new LoanExtensionRequest(loan, ticket.getSenderSub());
        loanExtensionRequestRepository.save(extensionRequest);

        logger.info("Nieuwe verlengingsaanvraag opgeslagen voor loanId {}: Lener {} vraagt verlenging aan voor de school.",
                loanId, ticket.getSenderSub());
    }

    @Transactional(readOnly = true)
    public long getPendingExtensionRequestsCount(Long schoolId) {
        return loanExtensionRequestRepository.countByLoan_Copy_Book_School_IdAndStatus(schoolId,
                LoanExtensionRequest.RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<LoanExtensionRequestDto> getPendingExtensionRequests(Long schoolId) {
        return loanExtensionRequestRepository
                .findByLoan_Copy_Book_School_IdAndStatus(schoolId, LoanExtensionRequest.RequestStatus.PENDING)
                .stream()
                .map(this::toExtensionDto)
                .collect(Collectors.toList());
    }

    private LoanExtensionRequestDto toExtensionDto(LoanExtensionRequest req) {
        LoanExtensionRequestDto dto = new LoanExtensionRequestDto();
        dto.setId(req.getId());
        dto.setLoanId(req.getLoan().getId());
        dto.setBookTitle(req.getLoan().getCopy().getBook().getTitel());
        dto.setBookCover(req.getLoan().getCopy().getBook().getCover());
        dto.setRequesterSub(req.getRequesterUserSub());
        dto.setRequestDate(req.getRequestDate());
        dto.setStatus(req.getStatus());
        dto.setCurrentDueDate(req.getLoan().getDueDate());
        dto.setNewDueDate(req.getNewDueDate());
        dto.setLibrarianNotes(req.getLibrarianNotes());

        // Naam wordt hier dynamisch opgelost, NIET uit de database tabel
        // 'loan_extension_requests'
        displayNameResolver.peek(req.getRequesterUserSub()).ifPresent(dto::setRequesterName);
        return dto;
    }

    @Transactional
    public void approveExtensionRequest(Long requestId, String librarianSub, LocalDate newDueDate) {
        LoanExtensionRequest req = loanExtensionRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new ApiException("Aanvraag niet gevonden", HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));

        if (req.getStatus() != LoanExtensionRequest.RequestStatus.PENDING) {
            throw new ApiException("Aanvraag is al verwerkt", HttpStatus.CONFLICT, "REQUEST_ALREADY_PROCESSED");
        }

        LocalDate finalDueDate = (newDueDate != null) ? newDueDate : req.getLoan().getDueDate().plusDays(14);
        req.getLoan().setDueDate(finalDueDate);
        req.setStatus(LoanExtensionRequest.RequestStatus.APPROVED);
        req.setProcessedByLibrarianSub(librarianSub);
        req.setProcessedDate(java.time.LocalDateTime.now());
        req.setNewDueDate(finalDueDate);

        loanRepo.save(req.getLoan());
        loanExtensionRequestRepository.save(req);

        sendExtensionResponseSmartschoolMessage(req, true);
    }

    @Transactional
    public void rejectExtensionRequest(Long requestId, String librarianSub, String notes) {
        LoanExtensionRequest req = loanExtensionRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new ApiException("Aanvraag niet gevonden", HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));

        if (req.getStatus() != LoanExtensionRequest.RequestStatus.PENDING) {
            throw new ApiException("Aanvraag is al verwerkt", HttpStatus.CONFLICT, "REQUEST_ALREADY_PROCESSED");
        }

        req.setStatus(LoanExtensionRequest.RequestStatus.REJECTED);
        req.setProcessedByLibrarianSub(librarianSub);
        req.setProcessedDate(java.time.LocalDateTime.now());
        req.setLibrarianNotes(notes);

        loanExtensionRequestRepository.save(req);

        sendExtensionResponseSmartschoolMessage(req, false);
    }

    private void sendExtensionResponseSmartschoolMessage(LoanExtensionRequest req, boolean approved) {
        try {
            authService.getUserInfoBySub(req.getRequesterUserSub())
                    .flatMap(userInfo -> {
                        SmartschoolMessageRequest msg = new SmartschoolMessageRequest();
                        msg.setPlatformUrl(userInfo.getPlatform() != null ? userInfo.getPlatform()
                                : smartschoolProperties.getApiBaseUrl());
                        msg.setSubject(approved ? "Goedgekeurd: verlenging bibliotheekboek"
                                : "Afgewezen: verlenging bibliotheekboek");

                        String resultText = approved ? "goedgekeurd" : "afgewezen";
                        String extraInfo = approved
                                ? String.format(
                                        "<div style='background-color: #fff8e1; border-left: 4px solid #f0a500; padding: 14px; margin: 15px 0;'><strong>Nieuwe inleverdatum:</strong> %s</div>",
                                        req.getNewDueDate())
                                : String.format(
                                        "<div style='background-color: #f9f9f9; border-left: 4px solid #8b1a1a; padding: 14px; margin: 15px 0;'><strong>Reden:</strong> %s</div>",
                                        escapeHtml(req.getLibrarianNotes()));

                        String body = String.format(
                                """
                                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                                          <div style="background-color: %s; padding: 24px 32px;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: normal; letter-spacing: 0.5px;">Bibliotheek — Verlenging</h1>
                                          </div>
                                          <div style="padding: 28px 32px;">
                                            <p>Beste <strong>%s</strong>,</p>
                                            <p>Uw aanvraag voor de verlenging van <strong>%s</strong> is %s.</p>
                                            %s
                                            <p>Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
                                          </div>
                                        </div>
                                        """,
                                approved ? "#1a3a5c" : "#8b1a1a", escapeHtml(userInfo.getName()),
                                escapeHtml(req.getLoan().getCopy().getBook().getTitel()), resultText, extraInfo);
                        msg.setBody(body);
                        return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), msg);
                    }).subscribe();
        } catch (Exception e) {
            logger.warn("Kon Smartschool-antwoord niet versturen: {}", e.getMessage());
        }
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        return returnLoan(loanId, null);
    }

    @Transactional
    public LoanDto returnLoan(Long loanId, ReturnLoanRequest request) {
        Loan loan = loanRepo.findById(Objects.requireNonNull(loanId, "loanId"))
                .orElseThrow(() -> new IllegalArgumentException("Uitlening niet gevonden"));

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Boek al teruggegeven");
        }

        LocalDate returnedAt = LocalDate.now();
        BookCopy.CopyStatus targetStatus = resolveReturnedStatus(request);
        BookCopy.CopyCondition targetCondition = resolveReturnedCondition(request);

        // Count available copies BEFORE marking this one available aka a kind of
        // snapshot to check if the book just became available after this return
        long availableCopiesBefore = copyRepo.findByBook_Id(loan.getCopy().getBook().getId()).stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                        || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
                .count();

        loan.setReturnedAt(returnedAt);
        loan.setReturnedStatus(targetStatus);
        loan.setReturnedCondition(targetCondition);
        loan.getCopy().setStatus(targetStatus);
        loan.getCopy().setCondition(targetCondition);
        copyRepo.save(Objects.requireNonNull(loan.getCopy(), "copy"));

        // Check if book just became available (was 0, now 1+)
        if ((targetStatus == BookCopy.CopyStatus.AVAILABLE
                || targetStatus == BookCopy.CopyStatus.DAMAGED)
                && availableCopiesBefore == 0) {
            bookAvailabilityNotificationService.notifyWishlistersThatBookIsAvailable(loan.getCopy().getBook());
        }

        logger.info("Returned loan id={} with copy status {} and condition {}", loanId, targetStatus, targetCondition);
        return toDto(loanRepo.save(loan));
    }

    private BookCopy.CopyStatus resolveReturnedStatus(ReturnLoanRequest request) {
        if (request == null) {
            return BookCopy.CopyStatus.AVAILABLE;
        }

        if (request.isLost()) {
            return BookCopy.CopyStatus.LOST;
        }

        if (request.getCondition() == ReturnLoanRequest.ReturnCondition.MODERATE
                || request.getCondition() == ReturnLoanRequest.ReturnCondition.BAD) {
            return BookCopy.CopyStatus.DAMAGED;
        }

        return BookCopy.CopyStatus.AVAILABLE;
    }

    private BookCopy.CopyCondition resolveReturnedCondition(ReturnLoanRequest request) {
        if (request == null || request.getCondition() == null) {
            return BookCopy.CopyCondition.GOOD;
        }

        if (request.getCondition() == ReturnLoanRequest.ReturnCondition.MODERATE) {
            return BookCopy.CopyCondition.MODERATE;
        }

        if (request.getCondition() == ReturnLoanRequest.ReturnCondition.BAD) {
            return BookCopy.CopyCondition.BAD;
        }

        return BookCopy.CopyCondition.GOOD;
    }

    public List<LoanDto> getActiveLoansForUser(String userSub) {
        return loanRepo.findByUserSubAndReturnedAtIsNull(userSub)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoanDto> getLoanHistoryForUser(String userSub) {
        return loanRepo.findByUserSubAndReturnedAtIsNotNull(userSub)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoanDto> getActiveLoansForBook(Long bookId) {
        return loanRepo.findByCopy_Book_IdAndReturnedAtIsNull(bookId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoanDto> getAllActiveLoans() {
        return loanRepo.findByReturnedAtIsNull()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public LoanConditionOverviewDto getConditionOverview() {
        List<BookCopy> copies = copyRepo.findAll();
        Map<Long, Integer> copyNumbersByCopyId = buildCopyNumbersByCopyId(copies);

        List<LoanConditionOverviewDto.WorsenedReturnDto> worsenedReturns = loanRepo.findByReturnedAtIsNotNull()
                .stream()
                .filter(this::isWorsenedReturn)
                .map(loan -> {
                    LoanConditionOverviewDto.WorsenedReturnDto dto = new LoanConditionOverviewDto.WorsenedReturnDto();
                    dto.setLoanId(loan.getId());
                    dto.setCopyId(loan.getCopy().getId());
                    dto.setCopyNumber(copyNumbersByCopyId.get(loan.getCopy().getId()));
                    dto.setUserSub(loan.getUserSub());
                    // Resolve display name for the user
                    displayNameResolver.peek(loan.getUserSub()).ifPresent(dto::setUserDisplayName);

                    dto.setBookId(loan.getCopy().getBook().getId());

                    dto.setBookTitel(loan.getCopy().getBook().getTitel());
                    dto.setBookCover(loan.getCopy().getBook().getCover());
                    dto.setUserSub(loan.getUserSub());
                    dto.setLoanedAt(loan.getLoanedAt());
                    dto.setReturnedAt(loan.getReturnedAt());
                    dto.setLoanedCondition(loan.getLoanedCondition());
                    dto.setReturnedCondition(loan.getReturnedCondition());
                    dto.setReturnedStatus(loan.getReturnedStatus());
                    return dto;
                })
                .sorted(Comparator.comparing(LoanConditionOverviewDto.WorsenedReturnDto::getReturnedAt,
                        Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .collect(Collectors.toList());

        Map<Long, LoanConditionOverviewDto.BookStateDto> groupedStates = new LinkedHashMap<>();
        List<BookCopy> lostCopyEntities = new ArrayList<>();

        copies.forEach(copy -> {
            Long bookId = copy.getBook().getId();
            LoanConditionOverviewDto.BookStateDto state = groupedStates.computeIfAbsent(bookId, ignored -> {
                LoanConditionOverviewDto.BookStateDto newState = new LoanConditionOverviewDto.BookStateDto();
                newState.setBookId(copy.getBook().getId());
                newState.setBookTitel(copy.getBook().getTitel());
                newState.setBookCover(copy.getBook().getCover());
                return newState;
            });

            state.setTotalCopies(state.getTotalCopies() + 1);

            switch (copy.getStatus()) {
                case AVAILABLE -> state.setAvailableCopies(state.getAvailableCopies() + 1);
                case LOANED -> state.setLoanedCopies(state.getLoanedCopies() + 1);
                case DAMAGED -> state.setDamagedCopies(state.getDamagedCopies() + 1);
                case LOST -> {
                    state.setLostCopies(state.getLostCopies() + 1);
                    lostCopyEntities.add(copy);
                }
            }

            switch (copy.getCondition()) {
                case GOOD -> state.setGoodConditionCopies(state.getGoodConditionCopies() + 1);
                case MODERATE -> state.setModerateConditionCopies(state.getModerateConditionCopies() + 1);
                case BAD -> state.setBadConditionCopies(state.getBadConditionCopies() + 1);
            }
        });

        List<LoanConditionOverviewDto.BookStateDto> bookStates = new ArrayList<>(groupedStates.values());
        bookStates.sort(Comparator.comparing(LoanConditionOverviewDto.BookStateDto::getBookTitel,
                String.CASE_INSENSITIVE_ORDER));

        List<LoanConditionOverviewDto.LostCopyDto> lostCopies = lostCopyEntities.stream()
                .map(copy -> {
                    LoanConditionOverviewDto.LostCopyDto dto = new LoanConditionOverviewDto.LostCopyDto();
                    dto.setCopyId(copy.getId());
                    dto.setCopyNumber(copyNumbersByCopyId.get(copy.getId()));
                    dto.setBookId(copy.getBook().getId());
                    dto.setBookTitel(copy.getBook().getTitel());
                    dto.setBookCover(copy.getBook().getCover());
                    dto.setCondition(copy.getCondition());
                    return dto;
                })
                .sorted(Comparator.comparing(LoanConditionOverviewDto.LostCopyDto::getBookTitel,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        LoanConditionOverviewDto overview = new LoanConditionOverviewDto();
        overview.setWorsenedReturns(worsenedReturns);
        overview.setBookStates(bookStates);
        overview.setLostCopies(lostCopies);
        return overview;
    }

    private Map<Long, Integer> buildCopyNumbersByCopyId(List<BookCopy> copies) {
        Map<Long, List<BookCopy>> copiesByBook = copies.stream()
                .collect(Collectors.groupingBy(copy -> copy.getBook().getId()));
        Map<Long, Integer> copyNumbersByCopyId = new HashMap<>();

        copiesByBook.values().forEach(bookCopies -> {
            bookCopies.sort(Comparator.comparing(BookCopy::getId));
            for (int index = 0; index < bookCopies.size(); index++) {
                BookCopy copy = bookCopies.get(index);
                copyNumbersByCopyId.put(copy.getId(), index + 1);
            }
        });

        return copyNumbersByCopyId;
    }

    private boolean isWorsenedReturn(Loan loan) {
        if (loan.getReturnedStatus() == BookCopy.CopyStatus.LOST) {
            return true;
        }

        if (loan.getLoanedCondition() == null || loan.getReturnedCondition() == null) {
            return false;
        }

        return conditionSeverity(loan.getReturnedCondition()) > conditionSeverity(loan.getLoanedCondition());
    }

    private int conditionSeverity(BookCopy.CopyCondition condition) {
        if (condition == null) {
            return 0;
        }
        return switch (condition) {
            case GOOD -> 0;
            case MODERATE -> 1;
            case BAD -> 2;
        };
    }

    private LoanDto toDto(Loan loan) {
        LoanDto dto = new LoanDto();
        dto.setId(loan.getId());
        dto.setCopyId(loan.getCopy().getId());
        dto.setBookId(loan.getCopy().getBook().getId());
        dto.setBookTitel(loan.getCopy().getBook().getTitel());
        dto.setBookCover(loan.getCopy().getBook().getCover());
        if (loan.getCopy().getBook().getGenres() != null) {
            dto.setBookGenres(loan.getCopy().getBook().getGenres().stream().map(g -> g.getNaam())
                    .collect(Collectors.joining(", ")));
        }
        dto.setUserSub(loan.getUserSub());
        displayNameResolver.peek(loan.getUserSub()).ifPresent(dto::setUserDisplayName);
        dto.setLoanedAt(loan.getLoanedAt());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnedAt(loan.getReturnedAt());
        dto.setLoanedCondition(loan.getLoanedCondition());
        dto.setReturnedCondition(loan.getReturnedCondition());
        dto.setReturnedStatus(loan.getReturnedStatus());
        dto.setHasPendingExtensionRequest(loanExtensionRequestRepository.existsByLoan_IdAndStatus(loan.getId(),
                LoanExtensionRequest.RequestStatus.PENDING));
        return dto;
    }

    @Transactional
    public void updateDueDate(Long id, LocalDate newDate) {
        Loan loan = loanRepo.findById(Objects.requireNonNull(id, "id"))
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Can't edit deadline of the book");
        }

        loan.setDueDate(newDate);
        loanRepo.save(loan);
        logger.info("Updated due date for loan id={} to {}", id, newDate);
    }
}