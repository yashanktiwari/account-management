package com.accounting.util;

import com.accounting.dao.PartyDAO;
import com.accounting.model.Party;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PartyExcelImporter {

    private static final Logger log = AppLogger.get(PartyExcelImporter.class);

    // Column headers for the Excel template - only fields used in PartyMasterDialog
    private static final String[] COLUMN_HEADERS = {
        "Company Name", "Owner Name", "Mobile", "Email", "Address", "State", "City",
        "Pin Code", "GST", "PAN", "CST No", "TAN No", "TDS", "Aadhar No", "Routes"
    };

    /**
     * Generates and downloads an Excel template with the correct format for importing parties
     */
    public static void downloadTemplate(File outputFile) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            Sheet sheet = workbook.createSheet("Party Import Template");

            // Define column widths
            sheet.setColumnWidth(0, 25 * 256);  // Company Name
            sheet.setColumnWidth(1, 20 * 256);  // Owner Name
            sheet.setColumnWidth(2, 15 * 256);  // Mobile
            sheet.setColumnWidth(3, 25 * 256);  // Email
            sheet.setColumnWidth(4, 30 * 256);  // Address
            sheet.setColumnWidth(5, 15 * 256);  // State
            sheet.setColumnWidth(6, 20 * 256);  // City
            sheet.setColumnWidth(7, 12 * 256);  // Pin Code
            sheet.setColumnWidth(8, 20 * 256); // GST
            sheet.setColumnWidth(9, 15 * 256); // PAN
            sheet.setColumnWidth(10, 15 * 256); // CST No
            sheet.setColumnWidth(11, 15 * 256); // TAN No
            sheet.setColumnWidth(12, 12 * 256); // TDS
            sheet.setColumnWidth(13, 18 * 256); // Aadhar No
            sheet.setColumnWidth(14, 30 * 256); // Routes

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Create instruction style
            CellStyle instructionStyle = workbook.createCellStyle();
            Font instructionFont = workbook.createFont();
            instructionFont.setItalic(true);
            instructionFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            instructionStyle.setFont(instructionFont);
            instructionStyle.setWrapText(true);

            // Add instructions at the top
            Row instructionRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell instructionCell = instructionRow.createCell(0);
            instructionCell.setCellValue("Instructions:\n" +
                "- Fill in the party details in the columns below\n" +
                "- 'Company Name' is mandatory\n" +
                "- 'Routes' should be pipe-separated (e.g., Route1|Route2|Route3)\n" +
                "- Leave optional fields blank if not applicable\n" +
                "- Delete this instruction row before importing");
            instructionCell.setCellStyle(instructionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, COLUMN_HEADERS.length - 1));

            // Create header row
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < COLUMN_HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMN_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sample data row
            Row sampleRow = sheet.createRow(2);
            String[] sampleData = {
                "Sample Company Pvt Ltd", "John Doe", "9876543210", "sample@example.com",
                "123 Main Street", "Maharashtra", "Mumbai", "400001", "29ABCDE1234F1Z5",
                "ABCDE1234F", "CST123456", "TAN789012", "10", "1234-5678-9012",
                "Route1|Route2|Route3"
            };
            for (int i = 0; i < sampleData.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = sampleRow.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(dataStyle);
            }

            workbook.write(outputStream);
            log.info("Party import template generated: {}", outputFile.getAbsolutePath());
        }
    }

    /**
     * Imports parties from an Excel file
     * @param inputFile The Excel file to import from
     * @return Import result with success count, error count, and error messages
     */
    public static ImportResult importFromExcel(File inputFile) {
        ImportResult result = new ImportResult();
        PartyDAO dao = new PartyDAO();

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row (assuming row 0 is header, start from row 1)
            int startRow = 1;
            
            // Check if first row contains "Instructions" - skip it
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                org.apache.poi.ss.usermodel.Cell firstCell = firstRow.getCell(0);
                if (firstCell != null && firstCell.getStringCellValue().contains("Instructions")) {
                    startRow = 2; // Skip instruction row and header row
                }
            }

            for (int rowNum = startRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                try {
                    Party party = mapRowToParty(row);
                    if (party != null && party.getName() != null && !party.getName().trim().isEmpty()) {
                        dao.save(party);
                        result.successCount++;
                    }
                } catch (Exception e) {
                    result.errorCount++;
                    result.errorMessages.add("Row " + (rowNum + 1) + ": " + e.getMessage());
                    log.error("Error importing party from row " + (rowNum + 1), e);
                }
            }

            log.info("Party import completed. Success: {}, Errors: {}", result.successCount, result.errorCount);
        } catch (Exception e) {
            log.error("Failed to import parties from Excel", e);
            result.errorMessages.add("Failed to read Excel file: " + e.getMessage());
        }

        return result;
    }

    private static Party mapRowToParty(Row row) {
        Party party = new Party();

        // Helper method to safely get string value from cell
        java.util.function.Function<Integer, String> getCellValue = (colIndex) -> {
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIndex);
            if (cell == null) return null;
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    return String.valueOf((long) cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return null;
            }
        };

        party.setName(getCellValue.apply(0));           // Company Name (mandatory)
        party.setOwnerName(getCellValue.apply(1));      // Owner Name
        party.setMobile(getCellValue.apply(2));         // Mobile
        party.setEmail(getCellValue.apply(3));          // Email
        party.setAddress(getCellValue.apply(4));        // Address
        party.setState(getCellValue.apply(5));          // State
        party.setCity(getCellValue.apply(6));           // City
        party.setPincode(getCellValue.apply(7));        // Pin Code
        party.setGstin(getCellValue.apply(8));          // GST
        party.setPan(getCellValue.apply(9));            // PAN
        party.setCstNo(getCellValue.apply(10));        // CST No
        party.setTanNo(getCellValue.apply(11));         // TAN No
        party.setTds(getCellValue.apply(12));          // TDS
        party.setAadharNo(getCellValue.apply(13));      // Aadhar No
        party.setRoutes(getCellValue.apply(14));        // Routes (pipe-separated)
        party.setCreatedAt(LocalDateTime.now());
        party.setUpdatedAt(LocalDateTime.now());

        return party;
    }

    public static class ImportResult {
        public int successCount = 0;
        public int errorCount = 0;
        public final List<String> errorMessages = new ArrayList<>();

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public String getSummary() {
            return "Import completed: " + successCount + " parties imported successfully" +
                   (errorCount > 0 ? ", " + errorCount + " errors" : "");
        }
    }
}
