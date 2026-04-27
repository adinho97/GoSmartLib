package com.example.demo.services;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.dto.ReturnLoanRequest;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    private final LoanRepository loanRepo;
    private final BookCopyRepository copyRepo;
    private final BookAvailabilityNotificationService bookAvailabilityNotificationService;

    public LoanService(LoanRepository loanRepo, BookCopyRepository copyRepo,
            BookAvailabilityNotificationService bookAvailabilityNotificationService) {
        this.loanRepo = loanRepo;
        this.copyRepo = copyRepo;
        this.bookAvailabilityNotificationService = bookAvailabilityNotificationService;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request) {
        logger.info("Creating loan: bookId={}, copyId={}, userSub={}, dueDate={}",
            request.getBookId(), request.getCopyId(), request.getUserSub(), request.getDueDate());

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
        return toDto(savedLoan);
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

    public LoanConditionOverviewDto getConditionOverview() {
        List<LoanConditionOverviewDto.WorsenedReturnDto> worsenedReturns = loanRepo.findByReturnedAtIsNotNull()
                .stream()
                .filter(this::isWorsenedReturn)
                .map(loan -> {
                    LoanConditionOverviewDto.WorsenedReturnDto dto = new LoanConditionOverviewDto.WorsenedReturnDto();
                    dto.setLoanId(loan.getId());
                    dto.setCopyId(loan.getCopy().getId());
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
        copyRepo.findAll().forEach(copy -> {
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
                case LOST -> state.setLostCopies(state.getLostCopies() + 1);
            }

            switch (copy.getCondition()) {
                case GOOD -> state.setGoodConditionCopies(state.getGoodConditionCopies() + 1);
                case MODERATE -> state.setModerateConditionCopies(state.getModerateConditionCopies() + 1);
                case BAD -> state.setBadConditionCopies(state.getBadConditionCopies() + 1);
            }
        });

        List<LoanConditionOverviewDto.BookStateDto> bookStates = new ArrayList<>(groupedStates.values());
        bookStates.sort(Comparator.comparing(LoanConditionOverviewDto.BookStateDto::getBookTitel, String.CASE_INSENSITIVE_ORDER));

        List<LoanConditionOverviewDto.LostCopyDto> lostCopies = copyRepo.findAll().stream()
                .filter(copy -> copy.getStatus() == BookCopy.CopyStatus.LOST)
                .map(copy -> {
                    LoanConditionOverviewDto.LostCopyDto dto = new LoanConditionOverviewDto.LostCopyDto();
                    dto.setCopyId(copy.getId());
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