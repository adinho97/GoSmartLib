package com.example.demo.mappers;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;

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
        dto.setCover(book.getCover());
        dto.setBeschrijving(book.getBeschrijving());
        dto.setGenre(book.getGenre());
        dto.setUitgaveDatum(book.getUitgaveDatum());
        dto.setPaginas(book.getPaginas());
        dto.setTaal(book.getTaal());
        dto.setUitgeverij(book.getUitgeverij());
        if (book.getSchool() != null) {
            dto.setSchoolId(book.getSchool().getId());
            dto.setSchoolNaam(book.getSchool().getNaam());
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
        book.setCover(dto.getCover());
        book.setBeschrijving(dto.getBeschrijving());
        book.setGenre(dto.getGenre());
        book.setUitgaveDatum(dto.getUitgaveDatum());
        book.setPaginas(dto.getPaginas());
        book.setTaal(dto.getTaal());
        book.setUitgeverij(dto.getUitgeverij());

        return book;
    }
}
