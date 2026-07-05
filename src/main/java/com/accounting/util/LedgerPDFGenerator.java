package com.accounting.util;

import com.accounting.dao.LedgerDAO;
import com.accounting.model.Party;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LedgerPDFGenerator {

    private static final Logger log = AppLogger.get(LedgerPDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String IMAGES_FOLDER = "src/main/resources/images";

    // Fonts
    private static final Font F_BOLD_14 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font F_BOLD_12 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font F_BOLD_10 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font F_BOLD_9 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font F_NORM_10 = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font F_NORM_9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font F_NORM_8 = FontFactory.getFont(FontFactory.HELVETICA, 8);

    public static void generateLedgerPDF(Party party, LedgerDAO.LedgerResult ledgerResult, 
                                         LocalDate fromDate, LocalDate toDate, String outputPath) {
        try {
            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Add header image
            Image headerImg = loadImage("REPORT_HEADER");
            if (headerImg != null) {
                headerImg.scaleToFit(550, 100);
                headerImg.setAlignment(Image.ALIGN_CENTER);
                document.add(headerImg);
            }

            // Add horizontal line
            LineSeparator line = new LineSeparator();
            line.setLineWidth(1f);
            line.setLineColor(Color.GRAY);
            document.add(new Chunk(line));

            // Add title
            Paragraph title = new Paragraph("ACCOUNT STATEMENT", F_BOLD_14);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(5);
            document.add(title);

            // Add party name
            Paragraph partyName = new Paragraph("Party: " + party.getName(), F_BOLD_12);
            partyName.setAlignment(Element.ALIGN_CENTER);
            partyName.setSpacingAfter(5);
            document.add(partyName);

            // Add date range
            Paragraph dateRange = new Paragraph(
                "From: " + fromDate.format(DATE_FORMATTER) + "  To: " + toDate.format(DATE_FORMATTER), 
                F_BOLD_10);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(10);
            document.add(dateRange);

            // Add opening balance
            String openingBalText = formatBalance(ledgerResult.getOpeningBalance());
            Paragraph openingBal = new Paragraph("Opening Balance: " + openingBalText, F_BOLD_10);
            openingBal.setAlignment(Element.ALIGN_LEFT);
            openingBal.setSpacingAfter(5);
            document.add(openingBal);

            // Create ledger table
            float[] columnWidths = {8f, 12f, 18f, 18f, 15f, 15f, 18f, 25f};
            PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);

            // Table headers
            Color headerBg = new Color(200, 200, 200);
            addTableCell(table, "S.No", F_BOLD_9, headerBg, Element.ALIGN_CENTER);
            addTableCell(table, "Date", F_BOLD_9, headerBg, Element.ALIGN_CENTER);
            addTableCell(table, "Type", F_BOLD_9, headerBg, Element.ALIGN_CENTER);
            addTableCell(table, "Transaction No", F_BOLD_9, headerBg, Element.ALIGN_CENTER);
            addTableCell(table, "Debit", F_BOLD_9, headerBg, Element.ALIGN_RIGHT);
            addTableCell(table, "Credit", F_BOLD_9, headerBg, Element.ALIGN_RIGHT);
            addTableCell(table, "Balance", F_BOLD_9, headerBg, Element.ALIGN_RIGHT);
            addTableCell(table, "Remarks", F_BOLD_9, headerBg, Element.ALIGN_LEFT);

            // Table rows
            int serialNo = 1;
            double totalDebit = 0;
            double totalCredit = 0;

            for (LedgerDAO.LedgerEntry entry : ledgerResult.getEntries()) {
                totalDebit += entry.getDebit();
                totalCredit += entry.getCredit();

                addTableCell(table, String.valueOf(serialNo++), F_NORM_8, Color.WHITE, Element.ALIGN_CENTER);
                addTableCell(table, entry.getDate().format(DATE_FORMATTER), F_NORM_8, Color.WHITE, Element.ALIGN_CENTER);
                addTableCell(table, entry.getTransactionType(), F_NORM_8, Color.WHITE, Element.ALIGN_LEFT);
                addTableCell(table, entry.getTransactionNo(), F_NORM_8, Color.WHITE, Element.ALIGN_LEFT);
                addTableCell(table, formatAmount(entry.getDebit()), F_NORM_8, Color.WHITE, Element.ALIGN_RIGHT);
                addTableCell(table, formatAmount(entry.getCredit()), F_NORM_8, Color.WHITE, Element.ALIGN_RIGHT);
                addTableCell(table, formatBalance(entry.getRunningBalance()), F_NORM_8, Color.WHITE, Element.ALIGN_RIGHT);
                addTableCell(table, entry.getRemarks() != null ? entry.getRemarks() : "", F_NORM_8, Color.WHITE, Element.ALIGN_LEFT);
            }

            // Add totals row
            Color totalBg = new Color(230, 230, 230);
            addTableCell(table, "", F_BOLD_9, totalBg, Element.ALIGN_CENTER);
            addTableCell(table, "", F_BOLD_9, totalBg, Element.ALIGN_CENTER);
            addTableCell(table, "", F_BOLD_9, totalBg, Element.ALIGN_CENTER);
            addTableCell(table, "Total:", F_BOLD_9, totalBg, Element.ALIGN_RIGHT);
            addTableCell(table, formatAmount(totalDebit), F_BOLD_9, totalBg, Element.ALIGN_RIGHT);
            addTableCell(table, formatAmount(totalCredit), F_BOLD_9, totalBg, Element.ALIGN_RIGHT);
            addTableCell(table, "", F_BOLD_9, totalBg, Element.ALIGN_CENTER);
            addTableCell(table, "", F_BOLD_9, totalBg, Element.ALIGN_CENTER);

            document.add(table);

            // Add closing balance
            String closingBalText = formatBalance(ledgerResult.getClosingBalance());
            Paragraph closingBal = new Paragraph("Closing Balance: " + closingBalText, F_BOLD_10);
            closingBal.setAlignment(Element.ALIGN_RIGHT);
            closingBal.setSpacingBefore(10);
            document.add(closingBal);

            document.close();
            log.info("Ledger PDF generated: {}", outputPath);

        } catch (Exception e) {
            log.error("Failed to generate ledger PDF", e);
            throw new RuntimeException("Failed to generate ledger PDF: " + e.getMessage(), e);
        }
    }

    private static void addTableCell(PdfPTable table, String text, Font font, Color backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        cell.setBorderColor(Color.GRAY);
        table.addCell(cell);
    }

    private static String formatAmount(double amount) {
        if (amount == 0) return "-";
        return String.format("%,.2f", amount);
    }

    private static String formatBalance(double balance) {
        String suffix = balance >= 0 ? " Cr" : " Dr";
        return String.format("%,.2f%s", Math.abs(balance), suffix);
    }

    private static Image loadImage(String name) {
        String[] extensions = {".jpg", ".JPG", ".jpeg", ".JPEG", ".png", ".PNG"};
        for (String ext : extensions) {
            File f = new File(IMAGES_FOLDER + "/" + name + ext);
            if (f.exists()) {
                try {
                    return Image.getInstance(f.getAbsolutePath());
                } catch (Exception e) {
                    log.error("Failed to load image: " + name + ext, e);
                }
            }
        }
        log.warn("Image not found: {}", name);
        return null;
    }
}
