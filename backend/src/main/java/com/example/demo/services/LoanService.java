package com.example.demo.services;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanDto;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepo;
    private final BookCopyRepository copyRepo;

    public LoanService(LoanRepository loanRepo, BookCopyRepository copyRepo) {
        this.loanRepo = loanRepo;
        this.copyRepo = copyRepo;
    }

    @Transactional
    public LoanDto createLoan(CreateLoanRequest request) {
        BookCopy copy = copyRepo.findByBook_Id(request.getBookId())
                .stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Geen beschikbare exemplaren"));

        copy.setStatus(BookCopy.CopyStatus.LOANED);
        copyRepo.save(copy);

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setUserSub(request.getUserSub());
        loan.setLoanedAt(LocalDate.now());
        loan.setDueDate(request.getDueDate());

        return toDto(loanRepo.save(loan));
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Uitlening niet gevonden"));

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Boek al teruggegeven");
        }

        loan.setReturnedAt(LocalDate.now());
        loan.getCopy().setStatus(BookCopy.CopyStatus.AVAILABLE);
        copyRepo.save(loan.getCopy());

        return toDto(loanRepo.save(loan));
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
}