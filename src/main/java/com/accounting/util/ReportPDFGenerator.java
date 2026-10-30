package com.accounting.util;

import com.accounting.dao.ReportDAO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportPDFGenerator {

    private static final Logger log = AppLogger.get(ReportPDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String IMAGES_FOLDER = "src/main/resources/images";

    // Fonts
    private static final Font F_BOLD_14 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font F_BOLD_12 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font F_BOLD_10 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font F_NORM_10 = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font F_NORM_9 = FontFactory.getFont(FontFactory.HELVETICA, 9);

    public static void generateReportPDF(List<ReportDAO.ReportRow> rows, LocalDate fromDate, LocalDate toDate, String outputPath) {
        try {
            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Create outer table for the box
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);

            PdfPCell outerCell = new PdfPCell();
            outerCell.setBorder(Rectangle.BOX);
            outerCell.setBorderWidth(1.5f);
            outerCell.setPadding(5);

            // Add header image
            Image headerImg = loadImage("REPORT_HEADER");
            if (headerImg != null) {
                headerImg.scaleToFit(530, 100);
                headerImg.setAlignment(Image.ALIGN_CENTER);
                outerCell.addElement(headerImg);
            }

            // Add horizontal line
            LineSeparator line = new LineSeparator();
            line.setLineWidth(1f);
            line.setLineColor(Color.GRAY);
            outerCell.addElement(line);

            // Add title
            Paragraph title = new Paragraph("Account Statement", F_BOLD_14);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(5);
            title.setSpacingAfter(3);
            outerCell.addElement(title);

            // Add date range with separate From and To
            PdfPTable dateTable = new PdfPTable(2);
            dateTable.setWidthPercentage(60);
            dateTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell fromCell = new PdfPCell(new Phrase("From: " + fromDate.format(DATE_FORMATTER), F_BOLD_12));
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            fromCell.setPaddingRight(10);
            fromCell.setPaddingTop(2);
            fromCell.setPaddingBottom(2);

            PdfPCell toCell = new PdfPCell(new Phrase("To: " + toDate.format(DATE_FORMATTER), F_BOLD_12));
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            toCell.setPaddingLeft(10);
            toCell.setPaddingTop(2);
            toCell.setPaddingBottom(2);

            dateTable.addCell(fromCell);
            dateTable.addCell(toCell);
            dateTable.setSpacingAfter(3);
            outerCell.addElement(dateTable);

            // Add record count
            String count = "Total Records: " + rows.size();
            Paragraph countPara = new Paragraph(count, F_NORM_10);
            countPara.setAlignment(Element.ALIGN_CENTER);
            countPara.setSpacingAfter(5);
            outerCell.addElement(countPara);

            // Create table
            if (!rows.isEmpty()) {
                PdfPTable table = new PdfPTable(8); // 8 columns
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.5f, 1.5f, 1.2f, 1.0f, 1.2f, 0.8f, 0.8f, 1.5f});
                table.setSpacingBefore(0);

                // Header row
                addTableCell(table, "S.No", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
                addTableCell(table, "Type", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
                addTableCell(table, "Transaction No", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
                addTableCell(table, "Date", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
                addTableCell(table, "Party", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
                addTableCell(table, "Debit", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_RIGHT);
                addTableCell(table, "Credit", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_RIGHT);
                addTableCell(table, "Remarks", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);

                // Data rows
                int serialNo = 1;
                for (ReportDAO.ReportRow row : rows) {
                    addTableCell(table, String.valueOf(serialNo++), F_NORM_9, Color.WHITE, Element.ALIGN_CENTER);
                    addTableCell(table, formatTransactionType(row.getTransactionType()), F_NORM_9, Color.WHITE, Element.ALIGN_LEFT);
                    addTableCell(table, row.getTransactionNo(), F_NORM_9, Color.WHITE, Element.ALIGN_LEFT);
                    addTableCell(table, formatDate(row.getDate()), F_NORM_9, Color.WHITE, Element.ALIGN_CENTER);
                    addTableCell(table, row.getParty(), F_NORM_9, Color.WHITE, Element.ALIGN_LEFT);
                    addTableCell(table, formatAmount(row.getDebit()), F_NORM_9, Color.WHITE, Element.ALIGN_RIGHT);
                    addTableCell(table, formatAmount(row.getCredit()), F_NORM_9, Color.WHITE, Element.ALIGN_RIGHT);
                    addTableCell(table, row.getRemarks(), F_NORM_9, Color.WHITE, Element.ALIGN_LEFT);
                }

                outerCell.addElement(table);
            } else {
                Paragraph noData = new Paragraph("No data available for the selected criteria.", F_NORM_10);
                noData.setAlignment(Element.ALIGN_CENTER);
                outerCell.addElement(noData);
            }

            outerTable.addCell(outerCell);
            document.add(outerTable);
            document.close();
            log.info("Report PDF generated: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Report PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private static void addTableCell(PdfPTable table, String text, Font font, Color backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(backgroundColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorderColor(Color.GRAY);
        table.addCell(cell);
    }

    private static String formatTransactionType(String type) {
        if (type == null) return "";
        return type.replace("Invoice", "Inv.").replace("Receipt", "Rcpt.");
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private static String formatAmount(double amount) {
        if (amount == 0) return "-";
        return String.format("%.2f", amount);
    }

    public static void generateOutstandingPDF(String reportTitle, Map<String, Double> partyAmounts, String outputPath) {
        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Header image
            Image headerImg = loadImage("REPORT_HEADER");
            if (headerImg != null) {
                headerImg.scaleToFit(520, 100);
                headerImg.setAlignment(Image.ALIGN_CENTER);
                document.add(headerImg);
            }

            LineSeparator line = new LineSeparator();
            line.setLineWidth(1f);
            line.setLineColor(Color.GRAY);
            document.add(new Chunk(line));

            // Title
            Paragraph title = new Paragraph(reportTitle, F_BOLD_14);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(12);
            document.add(title);

            // Table: Sr.No | Party Name | Amount
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.6f, 3.5f, 1.5f});
            table.setSpacingBefore(4);

            addTableCell(table, "Sr.No",      F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
            addTableCell(table, "Party Name", F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_LEFT);
            addTableCell(table, "Amount",     F_BOLD_10, Color.LIGHT_GRAY, Element.ALIGN_RIGHT);

            int serial = 1;
            double total = 0;
            List<Map.Entry<String, Double>> sorted = new ArrayList<>(partyAmounts.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> entry : sorted) {
                Color bg = (serial % 2 == 0) ? new Color(245, 245, 245) : Color.WHITE;
                addTableCell(table, String.valueOf(serial++), F_NORM_9, bg, Element.ALIGN_CENTER);
                addTableCell(table, entry.getKey(),           F_NORM_9, bg, Element.ALIGN_LEFT);
                addTableCell(table, String.format("\u20B9%.2f", entry.getValue()), F_NORM_9, bg, Element.ALIGN_RIGHT);
                total += entry.getValue();
            }

            // Total row
            addTableCell(table, "",      F_BOLD_10, new Color(220, 220, 220), Element.ALIGN_CENTER);
            addTableCell(table, "Total", F_BOLD_10, new Color(220, 220, 220), Element.ALIGN_LEFT);
            addTableCell(table, String.format("\u20B9%.2f", total), F_BOLD_10, new Color(220, 220, 220), Element.ALIGN_RIGHT);

            document.add(table);
            document.close();
            log.info("Outstanding PDF generated: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Outstanding PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
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
