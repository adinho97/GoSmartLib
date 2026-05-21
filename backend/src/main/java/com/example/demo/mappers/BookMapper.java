package com.example.demo.mappers;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.Leesniveau;
import com.example.demo.entities.BookCopy;

public class BookMapper {

    public static BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setTitel(book.getTitel());
        dto.setAuteur(book.getAuteur());
        dto.setIsbn(book.getIsbn());
        dto.setGoNumber(book.getGoNumber());
        dto.setCover(book.getCover());
        dto.setBeschrijving(book.getBeschrijving());
        dto.setGenre(book.getGenre());
        dto.setUitgaveDatum(book.getUitgaveDatum());
        dto.setPaginas(book.getPaginas());
        dto.setTaal(book.getTaal());
        dto.setUitgeverij(book.getUitgeverij());
        dto.setLeesniveau(Leesniveau.fromValue(book.getLeesniveau()));
        if (book.getSchool() != null) {
            dto.setSchoolId(book.getSchool().getId());
            dto.setSchoolNaam(book.getSchool().getNaam());
        }

        if (book.getReviews() != null) {
            int reviewCount = book.getReviews().size();
            dto.setReviewCount(reviewCount);

            if (reviewCount > 0) {
                double total = book.getReviews().stream()
                        .mapToDouble(review -> review.getRating() != null ? review.getRating() : 0)
                        .sum();
                dto.setAverageRating(total / reviewCount);
            }
        }
        if (book.getCopies() != null) {
            dto.setTotalCopies(book.getCopies().size());
            long available = book.getCopies().stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                    || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
                    .count();
            dto.setAvailableCopies((int) available);
        }

        return dto;
    }

    public static Book toEntity(BookDto dto) {
        if (dto == null) {
            return null;
        }

        Book book = new Book();
        book.setId(dto.getId());
        book.setTitel(dto.getTitel());
        book.setAuteur(dto.getAuteur());
        book.setIsbn(dto.getIsbn());
        book.setGoNumber(dto.getGoNumber());
        book.setCover(dto.getCover());
        book.setBeschrijving(dto.getBeschrijving());
        book.setGenre(dto.getGenre());
        book.setUitgaveDatum(dto.getUitgaveDatum());
        book.setPaginas(dto.getPaginas());
        book.setTaal(dto.getTaal());
        book.setUitgeverij(dto.getUitgeverij());
        if (dto.getLeesniveau() != null) {
            book.setLeesniveau(dto.getLeesniveau().getLabel());
        }

        return book;
    }
}
