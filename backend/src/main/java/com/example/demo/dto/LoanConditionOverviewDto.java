package com.example.demo.dto;

import com.example.demo.entities.BookCopy;

import java.time.LocalDate;
import java.util.List;

public class LoanConditionOverviewDto {

    private List<WorsenedReturnDto> worsenedReturns;
    private List<BookStateDto> bookStates;
    private List<LostCopyDto> lostCopies;

    public List<WorsenedReturnDto> getWorsenedReturns() {
        return worsenedReturns;
    }

    public void setWorsenedReturns(List<WorsenedReturnDto> worsenedReturns) {
        this.worsenedReturns = worsenedReturns;
    }

    public List<BookStateDto> getBookStates() {
        return bookStates;
    }

    public void setBookStates(List<BookStateDto> bookStates) {
        this.bookStates = bookStates;
    }

    public List<LostCopyDto> getLostCopies() {
        return lostCopies;
    }

    public void setLostCopies(List<LostCopyDto> lostCopies) {
        this.lostCopies = lostCopies;
    }

    public static class WorsenedReturnDto {
        private Long loanId;
        private Long copyId;
        private Integer copyNumber;
        private Long bookId;
        private String bookTitel;
        private String bookCover;
        private String userSub;
        private String userDisplayName;
        private LocalDate loanedAt;
        private LocalDate returnedAt;
        private BookCopy.CopyCondition loanedCondition;
        private BookCopy.CopyCondition returnedCondition;
        private BookCopy.CopyStatus returnedStatus;

        public Long getLoanId() {
            return loanId;
        }

        public void setLoanId(Long loanId) {
            this.loanId = loanId;
        }

        public Long getCopyId() {
            return copyId;
        }

        public void setCopyId(Long copyId) {
            this.copyId = copyId;
        }

        public Integer getCopyNumber() {
            return copyNumber;
        }

        public void setCopyNumber(Integer copyNumber) {
            this.copyNumber = copyNumber;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public String getBookTitel() {
            return bookTitel;
        }

        public void setBookTitel(String bookTitel) {
            this.bookTitel = bookTitel;
        }

        public String getBookCover() {
            return bookCover;
        }

        public void setBookCover(String bookCover) {
            this.bookCover = bookCover;
        }

        public String getUserSub() {
            return userSub;
        }

        public void setUserSub(String userSub) {
            this.userSub = userSub;
        }

        public String getUserDisplayName() {
            return userDisplayName;
        }

        public void setUserDisplayName(String userDisplayName) {
            this.userDisplayName = userDisplayName;
        }

        public LocalDate getLoanedAt() {
            return loanedAt;
        }

        public void setLoanedAt(LocalDate loanedAt) {
            this.loanedAt = loanedAt;
        }

        public LocalDate getReturnedAt() {
            return returnedAt;
        }

        public void setReturnedAt(LocalDate returnedAt) {
            this.returnedAt = returnedAt;
        }

        public BookCopy.CopyCondition getLoanedCondition() {
            return loanedCondition;
        }

        public void setLoanedCondition(BookCopy.CopyCondition loanedCondition) {
            this.loanedCondition = loanedCondition;
        }

        public BookCopy.CopyCondition getReturnedCondition() {
            return returnedCondition;
        }

        public void setReturnedCondition(BookCopy.CopyCondition returnedCondition) {
            this.returnedCondition = returnedCondition;
        }

        public BookCopy.CopyStatus getReturnedStatus() {
            return returnedStatus;
        }

        public void setReturnedStatus(BookCopy.CopyStatus returnedStatus) {
            this.returnedStatus = returnedStatus;
        }
    }

    public static class BookStateDto {
        private Long bookId;
        private String bookTitel;
        private String bookCover;
        private long totalCopies;
        private long availableCopies;
        private long loanedCopies;
        private long damagedCopies;
        private long lostCopies;
        private long goodConditionCopies;
        private long moderateConditionCopies;
        private long badConditionCopies;

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public String getBookTitel() {
            return bookTitel;
        }

        public void setBookTitel(String bookTitel) {
            this.bookTitel = bookTitel;
        }

        public String getBookCover() {
            return bookCover;
        }

        public void setBookCover(String bookCover) {
            this.bookCover = bookCover;
        }

        public long getTotalCopies() {
            return totalCopies;
        }

        public void setTotalCopies(long totalCopies) {
            this.totalCopies = totalCopies;
        }

        public long getAvailableCopies() {
            return availableCopies;
        }

        public void setAvailableCopies(long availableCopies) {
            this.availableCopies = availableCopies;
        }

        public long getLoanedCopies() {
            return loanedCopies;
        }

        public void setLoanedCopies(long loanedCopies) {
            this.loanedCopies = loanedCopies;
        }

        public long getDamagedCopies() {
            return damagedCopies;
        }

        public void setDamagedCopies(long damagedCopies) {
            this.damagedCopies = damagedCopies;
        }

        public long getLostCopies() {
            return lostCopies;
        }

        public void setLostCopies(long lostCopies) {
            this.lostCopies = lostCopies;
        }

        public long getGoodConditionCopies() {
            return goodConditionCopies;
        }

        public void setGoodConditionCopies(long goodConditionCopies) {
            this.goodConditionCopies = goodConditionCopies;
        }

        public long getModerateConditionCopies() {
            return moderateConditionCopies;
        }

        public void setModerateConditionCopies(long moderateConditionCopies) {
            this.moderateConditionCopies = moderateConditionCopies;
        }

        public long getBadConditionCopies() {
            return badConditionCopies;
        }

        public void setBadConditionCopies(long badConditionCopies) {
            this.badConditionCopies = badConditionCopies;
        }
    }

    public static class LostCopyDto {
        private Long copyId;
        private Integer copyNumber;
        private Long bookId;
        private String bookTitel;
        private String bookCover;
        private BookCopy.CopyCondition condition;

        public Long getCopyId() {
            return copyId;
        }

        public void setCopyId(Long copyId) {
            this.copyId = copyId;
        }

        public Integer getCopyNumber() {
            return copyNumber;
        }

        public void setCopyNumber(Integer copyNumber) {
            this.copyNumber = copyNumber;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public String getBookTitel() {
            return bookTitel;
        }

        public void setBookTitel(String bookTitel) {
            this.bookTitel = bookTitel;
        }

        public String getBookCover() {
            return bookCover;
        }

        public void setBookCover(String bookCover) {
            this.bookCover = bookCover;
        }

        public BookCopy.CopyCondition getCondition() {
            return condition;
        }

        public void setCondition(BookCopy.CopyCondition condition) {
            this.condition = condition;
        }
    }
}
