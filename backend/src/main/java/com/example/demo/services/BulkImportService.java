package com.example.demo.services;

import com.example.demo.dto.ImportResultDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BulkImportService {

    private static final int BULK_IMPORT_MAX_ROWS = 200;

    private final IsbnService isbnService;

    public BulkImportService(IsbnService isbnService) {
        this.isbnService = isbnService;
    }

    public record IsbnQuantityPair(String isbn, int quantity) {
    }

    public ParsedBulkIsbn parseAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is required");
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);
        boolean isCsv = lowerName.endsWith(".csv");
        boolean isXls = lowerName.endsWith(".xls");
        boolean isXlsx = lowerName.endsWith(".xlsx");
        if (!isCsv && !isXls && !isXlsx) {
            throw new IllegalArgumentException("Unsupported file type. Use CSV, XLS or XLSX.");
        }

        ParsedBulkIsbn parsed = isCsv ? parseCsvIsbns(file) : parseExcelIsbns(file);

        if (parsed.totalRows() > BULK_IMPORT_MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Te veel rijen in upload: maximaal " + BULK_IMPORT_MAX_ROWS + " ISBN's per bestand.");
        }

        return parsed;
    }

    private ParsedBulkIsbn parseCsvIsbns(MultipartFile file) {
        List<IsbnQuantityPair> isbnQuantityPairs = new ArrayList<>();
        List<ImportResultDto.RowResult> invalidRows = new ArrayList<>();
        int totalRows = 0;
        int duplicateRowsSkipped = 0;

        try (InputStream in = file.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String[] columns = splitColumns(trimmedLine);
                String rawFirstColumn = columns[0].trim();

                if (lineNumber == 1 && rawFirstColumn.equalsIgnoreCase("isbn")) {
                    continue;
                }

                totalRows++;
                int quantity = 1;
                if (columns.length > 1 && !columns[1].trim().isEmpty()) {
                    try {
                        quantity = Integer.parseInt(columns[1].trim());
                        if (quantity <= 0) {
                            invalidRows.add(new ImportResultDto.RowResult(
                                    rawFirstColumn,
                                    ImportResultDto.Status.INVALID_ISBN,
                                    "Hoeveelheid moet groter dan 0 zijn",
                                    null));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        invalidRows.add(new ImportResultDto.RowResult(
                                rawFirstColumn,
                                ImportResultDto.Status.INVALID_ISBN,
                                "Ongeldig getal in hoeveelheid kolom",
                                null));
                        continue;
                    }
                }

                RowProcessResult rowResult = processParsedIsbnValue(
                        rawFirstColumn,
                        lineNumber,
                        quantity,
                        isbnQuantityPairs,
                        invalidRows,
                        true);
                if (rowResult == RowProcessResult.DUPLICATE) {
                    duplicateRowsSkipped++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file", e);
        }

        return new ParsedBulkIsbn(isbnQuantityPairs, invalidRows, totalRows, duplicateRowsSkipped);
    }

    private ParsedBulkIsbn parseExcelIsbns(MultipartFile file) {
        List<IsbnQuantityPair> isbnQuantityPairs = new ArrayList<>();
        List<ImportResultDto.RowResult> invalidRows = new ArrayList<>();
        int totalRows = 0;
        int duplicateRowsSkipped = 0;
        DataFormatter formatter = new DataFormatter();

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel file contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Cell cell = row.getCell(0);
                String rawFirstColumn = cell == null ? "" : formatter.formatCellValue(cell).trim();
                if (rawFirstColumn.isEmpty()) {
                    continue;
                }

                int lineNumber = rowIndex + 1;
                if (lineNumber == 1 && rawFirstColumn.equalsIgnoreCase("isbn")) {
                    continue;
                }

                totalRows++;
                int quantity = 1;
                Cell quantityCell = row.getCell(1);
                if (quantityCell != null) {
                    String quantityStr = formatter.formatCellValue(quantityCell).trim();
                    if (!quantityStr.isEmpty()) {
                        try {
                            quantity = Integer.parseInt(quantityStr);
                            if (quantity <= 0) {
                                invalidRows.add(new ImportResultDto.RowResult(
                                        rawFirstColumn,
                                        ImportResultDto.Status.INVALID_ISBN,
                                        "Hoeveelheid moet groter dan 0 zijn",
                                        null));
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            invalidRows.add(new ImportResultDto.RowResult(
                                    rawFirstColumn,
                                    ImportResultDto.Status.INVALID_ISBN,
                                    "Ongeldig getal in hoeveelheid kolom",
                                    null));
                            continue;
                        }
                    }
                }

                RowProcessResult rowResult = processParsedIsbnValue(
                        rawFirstColumn,
                        lineNumber,
                        quantity,
                        isbnQuantityPairs,
                        invalidRows,
                        false);
                if (rowResult == RowProcessResult.DUPLICATE) {
                    duplicateRowsSkipped++;
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read Excel file", ex);
        }

        return new ParsedBulkIsbn(isbnQuantityPairs, invalidRows, totalRows, duplicateRowsSkipped);
    }

    private String[] splitColumns(String line) {
        int comma = line.indexOf(',');
        int semicolon = line.indexOf(';');
        int tab = line.indexOf('\t');
        int space = line.indexOf(' ');

        int splitAt = Integer.MAX_VALUE;
        if (comma >= 0) {
            splitAt = Math.min(splitAt, comma);
        }
        if (semicolon >= 0) {
            splitAt = Math.min(splitAt, semicolon);
        }
        if (tab >= 0) {
            splitAt = Math.min(splitAt, tab);
        }
        if (space >= 0) {
            splitAt = Math.min(splitAt, space);
        }

        if (splitAt == Integer.MAX_VALUE) {
            return new String[] { line };
        }
        String firstColumn = line.substring(0, splitAt);
        String rest = line.substring(splitAt + 1);
        return new String[] { firstColumn, rest };
    }

    private RowProcessResult processParsedIsbnValue(
            String rawFirstColumn,
            int lineNumber,
            int quantity,
            List<IsbnQuantityPair> isbnQuantityPairs,
            List<ImportResultDto.RowResult> invalidRows,
            boolean treatEmptyAsInvalid) {
        if (rawFirstColumn.isEmpty()) {
            if (treatEmptyAsInvalid) {
                invalidRows.add(new ImportResultDto.RowResult(
                        "",
                        ImportResultDto.Status.INVALID_ISBN,
                        "Lege ISBN-waarde op rij " + lineNumber,
                        null));
            }
            return RowProcessResult.INVALID;
        }

        Optional<String> normalizedIsbn = isbnService.normalizeAndValidateIsbn(rawFirstColumn);
        if (normalizedIsbn.isEmpty()) {
            invalidRows.add(new ImportResultDto.RowResult(
                    rawFirstColumn,
                    ImportResultDto.Status.INVALID_ISBN,
                    "Ongeldig ISBN-formaat",
                    null));
            return RowProcessResult.INVALID;
        }

        String isbn = normalizedIsbn.get();
        boolean isDuplicate = isbnQuantityPairs.stream()
                .anyMatch(p -> p.isbn().equals(isbn));

        if (isDuplicate) {
            return RowProcessResult.DUPLICATE;
        }

        isbnQuantityPairs.add(new IsbnQuantityPair(isbn, quantity));
        return RowProcessResult.ADDED;
    }

    public record ParsedBulkIsbn(
            List<IsbnQuantityPair> isbnQuantityPairs,
            List<ImportResultDto.RowResult> invalidRows,
            int totalRows,
            int duplicateRowsSkipped) {
    }

    private enum RowProcessResult {
        ADDED,
        DUPLICATE,
        INVALID
    }
}
