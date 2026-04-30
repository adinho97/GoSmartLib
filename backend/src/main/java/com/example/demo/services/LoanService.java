package com.example.demo.services;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.dto.ReturnLoanRequest;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    private static final String LOGO_BASE64 = "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";
    private final LoanRepository loanRepo;
    private final BookCopyRepository copyRepo;
    private final BookAvailabilityNotificationService bookAvailabilityNotificationService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final AuthService authService;
    private final SmartschoolProperties smartschoolProperties;

    public LoanService(LoanRepository loanRepo, BookCopyRepository copyRepo,
            BookAvailabilityNotificationService bookAvailabilityNotificationService,
            SmartschoolMessageService smartschoolMessageService,
            AuthService authService,
            SmartschoolProperties smartschoolProperties) {
        this.loanRepo = loanRepo;
        this.copyRepo = copyRepo;
        this.bookAvailabilityNotificationService = bookAvailabilityNotificationService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.authService = authService;
        this.smartschoolProperties = smartschoolProperties;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request) {
        return createLoan(request, true);
    }

    @Transactional
    public List<LoanDto> createLoans(List<CreateLoanRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        String userSub = requests.get(0).getUserSub();
        if (requests.stream().anyMatch(request -> request.getUserSub() == null || !request.getUserSub().equals(userSub))) {
            throw new IllegalArgumentException("All loans in one batch must belong to the same user");
        }

        LocalDate dueDate = requests.get(0).getDueDate();
        if (requests.stream().anyMatch(request -> request.getDueDate() == null || !request.getDueDate().equals(dueDate))) {
            throw new IllegalArgumentException("All loans in one batch must have the same due date");
        }

        List<LoanDto> createdLoans = new ArrayList<>();
        for (CreateLoanRequest request : requests) {
            createdLoans.add(createLoan(request, false));
        }

        sendCombinedLoanConfirmationForDtos(userSub, createdLoans);
        return createdLoans;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request, boolean sendMessage) {
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
                        loan.getDueDate() != null ? loan.getDueDate().toString() : "onbekend"
                    ));

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                })
                .doOnSuccess(res -> logger.info("Sent loan confirmation to {} for {}", loan.getUserSub(),
                    loan.getCopy().getBook().getTitel()))
                .doOnError(err -> logger.error("Failed to send loan confirmation for {}: {}", loan.getUserSub(),
                    err.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
        } catch (Exception ex) {
            logger.warn("Exception while attempting to send Smartschool confirmation: {}", ex.getMessage());
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
                .doOnSuccess(res -> logger.info("Sent combined loan confirmation to {} for {} books", userSub, loans.size()))
                .doOnError(err -> logger.error("Failed to send combined loan confirmation for {}: {}", userSub, err.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
        } catch (Exception ex) {
            logger.warn("Exception while attempting to send combined Smartschool confirmation: {}", ex.getMessage());
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
                .doOnSuccess(res -> logger.info("Sent combined loan confirmation to {} for {} books", userSub, loans.size()))
                .doOnError(err -> logger.error("Failed to send combined loan confirmation for {}: {}", userSub, err.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
        } catch (Exception ex) {
            logger.warn("Exception while attempting to send combined Smartschool confirmation: {}", ex.getMessage());
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
                        dueDateStr
                    ));

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), req);
                })
                .doOnSuccess(res -> logger.info("Sent single loan confirmation to {} for {}", userSub, title))
                .doOnError(err -> logger.error("Failed to send single loan confirmation for {}: {}", userSub, err.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                .subscribe();
        } catch (Exception ex) {
            logger.warn("Exception while attempting to send Smartschool confirmation: {}", ex.getMessage());
        }
    }

    // ── HTML builders ──────────────────────────────────────────────────────────

    private String buildSingleLoanHtml(String name, String title, String dueDateStr) {
        return String.format("""
            <div style="font-family: Georgia, serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
              <div style="background-color: #1a3a5c; padding: 16px 32px;">
                <img src=\"data:image/png;base64,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\" alt=\"GO! Antwerpen\" style=\"height: 48px; width: auto; display: block;\" />
              </div>
              <div style="padding: 28px 32px;">
                <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                <p style="margin: 0 0 24px; font-size: 15px; color: #333;">Hieronder vindt u het boek dat u hebt geleend.</p>
                <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Georgia, serif;">
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
                  <p style="margin: 0; font-size: 14px; color: #7a5c00;"><strong>Terugbrengdatum:</strong> %s</p>
                  <p style="margin: 6px 0 0; font-size: 13px; color: #9a7a20;">Gelieve het boek op deze datum terug te brengen.</p>
                </div>
                <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
              </div>
              <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
              </div>
            </div>
            """, escapeHtml(name), escapeHtml(title), dueDateStr);
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
                rowColor, i + 1, escapeHtml(titles.get(i))
            ));
        }

        return String.format("""
            <div style="font-family: Georgia, serif; max-width: 600px; margin: 0 auto; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
              <div style="background-color: #1a3a5c; padding: 16px 32px;">
                <img src=\"data:image/png;base64,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\" alt=\"GO! Antwerpen\" style=\"height: 48px; width: auto; display: block;\" />
              </div>
              <div style="padding: 28px 32px;">
                <p style="margin: 0 0 16px; font-size: 15px; color: #333;">Beste <strong>%s</strong>,</p>
                <p style="margin: 0 0 24px; font-size: 15px; color: #333;">Hieronder vindt u een overzicht van de <strong>%d boeken</strong> die u hebt geleend.</p>
                <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-family: Georgia, serif;">
                  <thead>
                    <tr style="background-color: #1a3a5c; color: #fff;">
                      <th style="padding: 10px 12px; text-align: left; font-size: 13px; width: 40px;">#</th>
                      <th style="padding: 10px 12px; text-align: left; font-size: 13px;">Titel</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                <div style="background-color: #fff8e1; border-left: 4px solid #f0a500; padding: 14px 18px; border-radius: 3px; margin-bottom: 24px;">
                  <p style="margin: 0; font-size: 14px; color: #7a5c00;"><strong>Terugbrengdatum:</strong> %s</p>
                  <p style="margin: 6px 0 0; font-size: 13px; color: #9a7a20;">Gelieve alle boeken op deze datum terug te brengen.</p>
                </div>
                <p style="margin: 0; font-size: 14px; color: #555;">Met vriendelijke groeten,<br><strong>De bibliotheek</strong></p>
              </div>
              <div style="background-color: #f5f5f5; padding: 14px 32px; border-top: 1px solid #e0e0e0;">
                <p style="margin: 0; font-size: 12px; color: #999; text-align: center;">Dit is een automatisch gegenereerd bericht — gelieve niet te antwoorden.</p>
              </div>
            </div>
            """, escapeHtml(name), titles.size(), bookRows.toString(), dueDateStr);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        return returnLoan(loanId, null);
    }

    @Transactional
    public LoanDto returnLoan(Long loanId, ReturnLoanRequest request) {
        Loan loan = loanRepo.findById(loanId)
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
        copyRepo.save(loan.getCopy());

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
        bookStates.sort(Comparator.comparing(LoanConditionOverviewDto.BookStateDto::getBookTitel, String.CASE_INSENSITIVE_ORDER));

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
                .sorted(Comparator.comparing(LoanConditionOverviewDto.LostCopyDto::getBookTitel, String.CASE_INSENSITIVE_ORDER))
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
        dto.setUserSub(loan.getUserSub());
        dto.setLoanedAt(loan.getLoanedAt());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnedAt(loan.getReturnedAt());
        dto.setLoanedCondition(loan.getLoanedCondition());
        dto.setReturnedCondition(loan.getReturnedCondition());
        dto.setReturnedStatus(loan.getReturnedStatus());
        return dto;
    }

    @Transactional
    public void updateDueDate(Long id, LocalDate newDate) {
        Loan loan = loanRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Can't edit deadline of the book");
        }

        loan.setDueDate(newDate);
        loanRepo.save(loan);
        logger.info("Updated due date for loan id={} to {}", id, newDate);
    }
}
