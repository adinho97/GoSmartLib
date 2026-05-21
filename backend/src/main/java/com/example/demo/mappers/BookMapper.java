package com.example.demo.mappers;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.Genre;
import com.example.demo.repositories.GenreRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    private final GenreRepository genreRepository;

    public BookMapper(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }
    public BookDto toDto(Book book) {
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
        if (book.getGenres() != null) {
            dto.setGenres(book.getGenres().stream()
                               .map(Genre::getNaam)
                               .collect(Collectors.toList()));
        }
        dto.setUitgaveDatum(book.getUitgaveDatum());
        dto.setPaginas(book.getPaginas());
        dto.setTaal(book.getTaal());
        dto.setUitgeverij(book.getUitgeverij());
        dto.setLeesniveau(book.getLeesniveau());
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

    public Book toEntity(BookDto dto) {
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
        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            book.setGenres(dto.getGenres().stream()
                               .map(genreName -> genreRepository.findByNaamIgnoreCase(genreName)
                                                                .orElseGet(() -> {
                                                                    // Handle case where genre might not exist, or create it
                                                                    Genre newGenre = new Genre(); newGenre.setNaam(genreName); return genreRepository.save(newGenre);
                                                                }))
                               .collect(Collectors.toSet()));
        }
        book.setUitgaveDatum(dto.getUitgaveDatum());
        book.setPaginas(dto.getPaginas());
        book.setTaal(dto.getTaal());
        book.setUitgeverij(dto.getUitgeverij());
        book.setLeesniveau(dto.getLeesniveau());

        return book;
    }
}
