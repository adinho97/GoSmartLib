package com.example.demo;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    void testCreateLoan_Success() {
        // 1. Arrange
        CreateLoanRequest request = new CreateLoanRequest();
        request.setBookId(1L);
        request.setUsername("test-user");
        request.setDueDate(LocalDate.now().plusDays(14));

        // We bouwen de keten van achter naar voren op:
        
        // A. Het Boek
        Book mockBook = new Book();
        mockBook.setId(1L);

        // B. De Kopie (moet het boek kennen!)
        BookCopy mockCopy = new BookCopy();
        mockCopy.setId(101L);
        mockCopy.setBook(mockBook); // CRUCIAAL: Voorkomt de Book.getId() NullPointer

        // C. De Lening (moet de kopie kennen!)
        Loan savedLoan = new Loan();
        savedLoan.setId(500L);
        savedLoan.setCopy(mockCopy); // CRUCIAAL: Voorkomt de BookCopy.getId() NullPointer
        savedLoan.setDueDate(request.getDueDate());
        savedLoan.setUsername(request.getUsername());
        savedLoan.setLoanedAt(LocalDate.now());

        // Mock de repository calls
        when(bookCopyRepository.findByBook_Id(anyLong()))
            .thenReturn(Collections.singletonList(mockCopy));
        
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);
        
        // Mock de save van de bookCopy (als de service de status van de kopie aanpast naar 'uitgeleend')
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(mockCopy);

        // 2. Act & Assert
        assertDoesNotThrow(() -> {
            loanService.createLoan(request);
        });

        // Verificatie
        verify(loanRepository, times(1)).save(any(Loan.class));
        verify(bookCopyRepository, times(1)).findByBook_Id(1L);
    }
}