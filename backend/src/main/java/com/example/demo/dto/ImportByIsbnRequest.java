package com.example.demo.dto;

import com.example.demo.entities.BookCopy;
import jakarta.validation.constraints.NotBlank;

public class ImportByIsbnRequest {
    @NotBlank
    private String isbn;
    
    private BookCopy.CopyCondition copyCondition;
    
    private Integer copyQuantity = 1;

    public ImportByIsbnRequest() {
        this.copyCondition = BookCopy.CopyCondition.GOOD;
        this.copyQuantity = 1;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public BookCopy.CopyCondition getCopyCondition() {
        return copyCondition != null ? copyCondition : BookCopy.CopyCondition.GOOD;
    }

    public void setCopyCondition(BookCopy.CopyCondition copyCondition) {
        this.copyCondition = copyCondition;
    }

    public Integer getCopyQuantity() {
        return copyQuantity != null && copyQuantity > 0 ? copyQuantity : 1;
    }

    public void setCopyQuantity(Integer copyQuantity) {
        this.copyQuantity = copyQuantity;
    }
}
