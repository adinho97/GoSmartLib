package com.example.demo.services;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanDto;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.entities.LoanHistory;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanHistoryRepository;
import com.example.demo.repositories.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);
    private final LoanRepository loanRepo;
    private final LoanHistoryRepository loanHistoryRepo;
    private final BookCopyRepository copyRepo;
    private final BookAvailabilityNotificationService bookAvailabilityNotificationService;

    public LoanService(LoanRepository loanRepo, LoanHistoryRepository loanHistoryRepo, BookCopyRepository copyRepo,
            BookAvailabilityNotificationService bookAvailabilityNotificationService) {
        this.loanRepo = loanRepo;
        this.loanHistoryRepo = loanHistoryRepo;
        this.copyRepo = copyRepo;
        this.bookAvailabilityNotificationService = bookAvailabilityNotificationService;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request) {
        logger.info("Creating loan: bookId={}, userSub={}, dueDate={}",
                request.getBookId(), request.getUserSub(), request.getDueDate());

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

        List<BookCopy> availableCopies = copyRepo.findByBook_Id(request.getBookId())
                .stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (availableCopies.isEmpty()) {
            logger.warn("No available copies for bookId={}", request.getBookId());
            throw new IllegalStateException("Geen beschikbare exemplaren");
        }

        BookCopy copy = availableCopies.get(0);
        copy.setStatus(BookCopy.CopyStatus.LOANED);
        copyRepo.save(copy);
        logger.info("Marked copy {} as LOANED", copy.getId());

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setUserSub(request.getUserSub());
        loan.setLoanedAt(LocalDate.now());
        loan.setDueDate(request.getDueDate());

        Loan savedLoan = loanRepo.save(loan);
        logger.info("Loan created: id={}, bookId={}, userSub={}", savedLoan.getId(), request.getBookId(),
                request.getUserSub());
        return toDto(savedLoan);
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitlening niet gevonden"));

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Boek al teruggegeven");
        }

        LocalDate returnedAt = LocalDate.now();

        // Count available copies BEFORE marking this one available aka a kind of
        // snapshot to check if the book just became available after this return
        long availableCopiesBefore = copyRepo.countByBook_IdAndStatus(
                loan.getCopy().getBook().getId(),
                BookCopy.CopyStatus.AVAILABLE);

        loan.setReturnedAt(returnedAt);
        loan.getCopy().setStatus(BookCopy.CopyStatus.AVAILABLE);
        copyRepo.save(loan.getCopy());

        // Check if book just became available (was 0, now 1+)
        if (availableCopiesBefore == 0) {
            bookAvailabilityNotificationService.notifyWishlistersThatBookIsAvailable(loan.getCopy().getBook());
        }

        LoanHistory history = new LoanHistory();
        history.setLoanId(loan.getId());
        history.setCopyId(loan.getCopy().getId());
        history.setBookId(loan.getCopy().getBook().getId());
        history.setBookTitel(loan.getCopy().getBook().getTitel());
        history.setBookCover(loan.getCopy().getBook().getCover());
        history.setUserSub(loan.getUserSub());
        history.setLoanedAt(loan.getLoanedAt());
        history.setDueDate(loan.getDueDate());
        history.setReturnedAt(returnedAt);

        LoanHistory savedHistory = loanHistoryRepo.save(history);
        loanRepo.delete(loan);

        return toDto(savedHistory);
    }

    public List<LoanDto> getActiveLoansForUser(String userSub) {
        return loanRepo.findByUserSubAndReturnedAtIsNull(userSub)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LoanDto> getLoanHistoryForUser(String userSub) {
        List<LoanDto> history = new ArrayList<>(loanHistoryRepo.findByUserSubOrderByReturnedAtDesc(userSub)
                .stream().map(this::toDto).collect(Collectors.toList()));

        // Keep older returned rows from loans visible during transition.
        history.addAll(loanRepo.findByUserSubAndReturnedAtIsNotNull(userSub)
                .stream().map(this::toDto).collect(Collectors.toList()));

        history.sort(
                Comparator.comparing(LoanDto::getReturnedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LoanDto::getLoanedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return history;
    }

    public List<LoanDto> getActiveLoansForBook(Long bookId) {
        return loanRepo.findByCopy_Book_IdAndReturnedAtIsNull(bookId)
                .stream().map(this::toDto).collect(Collectors.toList());
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
        return dto;
    }

    private LoanDto toDto(LoanHistory history) {
        LoanDto dto = new LoanDto();
        dto.setId(history.getLoanId() != null ? history.getLoanId() : history.getId());
        dto.setCopyId(history.getCopyId());
        dto.setBookId(history.getBookId());
        dto.setBookTitel(history.getBookTitel());
        dto.setBookCover(history.getBookCover());
        dto.setUserSub(history.getUserSub());
        dto.setLoanedAt(history.getLoanedAt());
        dto.setDueDate(history.getDueDate());
        dto.setReturnedAt(history.getReturnedAt());
        return dto;
    }
}