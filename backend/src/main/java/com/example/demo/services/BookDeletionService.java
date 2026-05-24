package com.example.demo.services;

import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Loan;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ClassReadingListItemRepository;
import com.example.demo.repositories.HighlightedBookRepository;
import com.example.demo.repositories.LeeslijstRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.WishlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookDeletionService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final LeeslijstRepository leeslijstRepository;
    private final WishlistRepository wishlistRepository;
    private final HighlightedBookRepository highlightedBookRepository;
    private final ClassReadingListItemRepository classReadingListItemRepository;

    public BookDeletionService(BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository,
            LeeslijstRepository leeslijstRepository,
            WishlistRepository wishlistRepository,
            HighlightedBookRepository highlightedBookRepository,
            ClassReadingListItemRepository classReadingListItemRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.leeslijstRepository = leeslijstRepository;
        this.wishlistRepository = wishlistRepository;
        this.highlightedBookRepository = highlightedBookRepository;
        this.classReadingListItemRepository = classReadingListItemRepository;
    }

    @Transactional
    public void deleteBook(Long id, Long schoolId) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        if (schoolId != null && (book.getSchool() == null || !book.getSchool().getId().equals(schoolId))) {
            throw new ApiException("Dit boek behoort niet tot jouw school.", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!loanRepository.findByCopy_Book_IdAndReturnedAtIsNull(id).isEmpty()) {
            throw new ApiException("Kan boek niet verwijderen: er zijn nog actieve uitleningen.", HttpStatus.CONFLICT,
                    "ACTIVE_LOANS_EXIST");
        }

        List<Loan> bookLoans = loanRepository.findAll().stream()
                .filter(loan -> loan.getCopy().getBook().getId().equals(id))
                .toList();
        loanRepository.deleteAll(bookLoans);

        if (wishlistRepository != null) {
            var wishes = wishlistRepository.findAll().stream()
                    .filter(wish -> wish.getBook().getId().equals(id))
                    .toList();
            wishlistRepository.deleteAll(wishes);
        }

        if (highlightedBookRepository != null) {
            var highlights = highlightedBookRepository.findAll().stream()
                    .filter(highlight -> id.equals(highlight.getBookId()))
                    .toList();
            highlightedBookRepository.deleteAll(highlights);
        }

        if (classReadingListItemRepository != null) {
            var classItems = classReadingListItemRepository.findAll().stream()
                    .filter(item -> id.equals(item.getBookId()))
                    .toList();
            classReadingListItemRepository.deleteAll(classItems);
        }

        if (leeslijstRepository != null) {
            leeslijstRepository.findAll().forEach(list -> {
                Set<Book> booksInList = new HashSet<>(list.getBooks());
                boolean changed = booksInList.removeIf(b -> b.getId().equals(id));
                if (changed) {
                    list.setBooks(booksInList);
                    leeslijstRepository.save(list);
                }
            });
            leeslijstRepository.flush();
        }

        List<BookCopy> bookCopies = bookCopyRepository.findAll().stream()
                .filter(copy -> copy.getBook().getId().equals(id))
                .toList();
        bookCopyRepository.deleteAll(bookCopies);

        bookRepository.delete(book);
    }
}
