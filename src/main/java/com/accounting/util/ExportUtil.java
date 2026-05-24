package com.accounting.util;

import com.accounting.model.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportUtil {

    private static final Logger log = AppLogger.get(ExportUtil.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Invoice Register Excel ──
    public static void exportInvoicesToExcel(List<Invoice> data, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Invoice Register");

        Row header = sheet.createRow(0);
        String[] columns = {
                "Invoice Date", "Invoice No.", "Customer/Supplier Name", "Total Qty",
                "Total Amt", "Total Tax", "Grand Total", "Trans Type", "Tax Type",
                "Cash/Credit", "Voucher Type", "Vehicle No", "Remarks"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Invoice inv : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "");
            row.createCell(1).setCellValue(safe(inv.getInvoiceNo()));
            row.createCell(2).setCellValue(safe(inv.getAccountName()));
            row.createCell(3).setCellValue(inv.getTotalQty());
            row.createCell(4).setCellValue(inv.getTotalAmt());
            row.createCell(5).setCellValue(inv.getTotalTax());
            row.createCell(6).setCellValue(inv.getGrandTotal());
            row.createCell(7).setCellValue(safe(inv.getTransType()));
            row.createCell(8).setCellValue(safe(inv.getTaxType()));
            row.createCell(9).setCellValue(safe(inv.getCashCredit()));
            row.createCell(10).setCellValue(safe(inv.getVoucherType()));
            row.createCell(11).setCellValue(safe(inv.getVehicleNo()));
            row.createCell(12).setCellValue(safe(inv.getRemarks()));
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    // ── Invoice Register PDF ──
    public static void exportInvoicesToPDF(List<Invoice> data, String filePath) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            String[] headers = {
                    "Date", "Invoice No.", "Name", "Qty", "Amount", "Tax",
                    "Grand Total", "Type", "Tax Type"
            };

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(37, 99, 235));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (Invoice inv : data) {
                table.addCell(new Phrase(inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "", bodyFont));
                table.addCell(new Phrase(safe(inv.getInvoiceNo()), bodyFont));
                table.addCell(new Phrase(safe(inv.getAccountName()), bodyFont));
                table.addCell(new Phrase(String.format("%.3f", inv.getTotalQty()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", inv.getTotalAmt()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", inv.getTotalTax()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", inv.getGrandTotal()), bodyFont));
                table.addCell(new Phrase(safe(inv.getTransType()), bodyFont));
                table.addCell(new Phrase(safe(inv.getTaxType()), bodyFont));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            log.error("Invoice PDF export failed | path={}", filePath, e);
            AlertUtil.showError("Export Failed", "PDF export failed:\n" + e.getMessage());
        }
    }

    // ── Account Statement Excel ──
    public static void exportStatementToExcel(List<AccountTransaction> data, String accountName, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Account Statement");

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Account Statement: " + safe(accountName));

        Row header = sheet.createRow(2);
        String[] columns = {
                "Date", "Particulars", "Voucher Type", "Voucher No",
                "Debit", "Credit", "Balance", "Dr/Cr", "Due Days", "Remarks"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 3;
        for (AccountTransaction t : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(t.getDate() != null ? t.getDate().format(DATE_FMT) : "");
            row.createCell(1).setCellValue(safe(t.getParticulars()));
            row.createCell(2).setCellValue(safe(t.getVoucherType()));
            row.createCell(3).setCellValue(safe(t.getVoucherNo()));
            row.createCell(4).setCellValue(t.getDebit());
            row.createCell(5).setCellValue(t.getCredit());
            row.createCell(6).setCellValue(t.getBalance());
            row.createCell(7).setCellValue(safe(t.getBalanceType()));
            row.createCell(8).setCellValue(t.getDueDays());
            row.createCell(9).setCellValue(safe(t.getRemarks()));
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    // ── Account Statement PDF ──
    public static void exportStatementToPDF(List<AccountTransaction> data, String accountName, String filePath) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Account Statement: " + safe(accountName), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            String[] headers = {
                    "Date", "Particulars", "Voucher Type", "Voucher No",
                    "Debit", "Credit", "Balance", "Dr/Cr", "Due Days", "Remarks"
            };

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(37, 99, 235));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
            for (AccountTransaction t : data) {
                table.addCell(new Phrase(t.getDate() != null ? t.getDate().format(DATE_FMT) : "", bodyFont));
                table.addCell(new Phrase(safe(t.getParticulars()), bodyFont));
                table.addCell(new Phrase(safe(t.getVoucherType()), bodyFont));
                table.addCell(new Phrase(safe(t.getVoucherNo()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", t.getDebit()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", t.getCredit()), bodyFont));
                table.addCell(new Phrase(String.format("%.2f", t.getBalance()), bodyFont));
                table.addCell(new Phrase(safe(t.getBalanceType()), bodyFont));
                table.addCell(new Phrase(String.valueOf(t.getDueDays()), bodyFont));
                table.addCell(new Phrase(safe(t.getRemarks()), bodyFont));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            log.error("Statement PDF export failed | path={}", filePath, e);
            AlertUtil.showError("Export Failed", "PDF export failed:\n" + e.getMessage());
        }
    }

    // ── Outstanding Register Excel ──
    public static void exportOutstandingToExcel(List<OutstandingEntry> data, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Outstanding Register");

        Row header = sheet.createRow(0);
        String[] columns = {
                "Particulars", "Root/Area", "Mobile", "Amount", "Dr/Cr",
                "Bill No.", "Bill Date", "Due Days", "Bill Amt", "Pending Amt"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (OutstandingEntry entry : data) {
            Row accountRow = sheet.createRow(rowNum++);
            accountRow.createCell(0).setCellValue(safe(entry.getAccountName()));
            accountRow.createCell(2).setCellValue(safe(entry.getMobile()));
            accountRow.createCell(3).setCellValue(entry.getAmount());
            accountRow.createCell(4).setCellValue(safe(entry.getDrCr()));

            for (BillDetail bd : entry.getBillDetails()) {
                Row billRow = sheet.createRow(rowNum++);
                billRow.createCell(5).setCellValue(safe(bd.getBillNo()));
                billRow.createCell(6).setCellValue(bd.getDate() != null ? bd.getDate().format(DATE_FMT) : "");
                billRow.createCell(7).setCellValue(bd.getDueDays());
                billRow.createCell(8).setCellValue(bd.getBillAmt());
                billRow.createCell(9).setCellValue(bd.getPendingAmt());
            }
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    // ── Vehicle Master Excel ──
    public static void exportVehiclesToExcel(List<Vehicle> data, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Vehicles");

        Row header = sheet.createRow(0);
        String[] columns = {"Vehicle No.", "Vehicle Model", "Account Name"};

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Vehicle v : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(safe(v.getVehicleNo()));
            row.createCell(1).setCellValue(safe(v.getVehicleModel()));
            row.createCell(2).setCellValue(safe(v.getAccountName()));
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    // ── Accounts Excel ──
    public static void exportAccountsToExcel(List<Account> data, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Accounts");

        Row header = sheet.createRow(0);
        String[] columns = {
                "Name", "Type", "Mailing Name", "Address", "State", "City",
                "Pin Code", "Email", "Mobile", "GSTIN", "PAN", "TDS(%)",
                "Credit Period", "Credit Limit", "Opening Balance", "Balance Type"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Account a : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(safe(a.getAccountName()));
            row.createCell(1).setCellValue(safe(a.getAccountType()));
            row.createCell(2).setCellValue(safe(a.getMailingName()));
            row.createCell(3).setCellValue(safe(a.getAddress()));
            row.createCell(4).setCellValue(safe(a.getStateName()));
            row.createCell(5).setCellValue(safe(a.getCityName()));
            row.createCell(6).setCellValue(safe(a.getPinCode()));
            row.createCell(7).setCellValue(safe(a.getEmail()));
            row.createCell(8).setCellValue(safe(a.getMobile()));
            row.createCell(9).setCellValue(safe(a.getGstin()));
            row.createCell(10).setCellValue(safe(a.getPanNo()));
            row.createCell(11).setCellValue(a.getTdsPercent());
            row.createCell(12).setCellValue(a.getCreditPeriod());
            row.createCell(13).setCellValue(a.getCreditAmtLimit());
            row.createCell(14).setCellValue(a.getOpeningBalance());
            row.createCell(15).setCellValue(safe(a.getBalanceType()));
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    // ── Payment Register Excel ──
    public static void exportPaymentsToExcel(List<Payment> data, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Payment Register");

        Row header = sheet.createRow(0);
        String[] columns = {
                "Date", "Voucher Type", "Voucher No", "Account Name",
                "Amount", "Against Invoice", "Remarks"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Payment p : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FMT) : "");
            row.createCell(1).setCellValue(safe(p.getVoucherType()));
            row.createCell(2).setCellValue(safe(p.getVoucherNo()));
            row.createCell(3).setCellValue(safe(p.getAccountName()));
            row.createCell(4).setCellValue(p.getAmount());
            row.createCell(5).setCellValue(safe(p.getAgainstInvoiceNo()));
            row.createCell(6).setCellValue(safe(p.getRemarks()));
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
