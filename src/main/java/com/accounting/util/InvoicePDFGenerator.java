package com.accounting.util;

import com.accounting.model.InvoiceLineItem;
import com.accounting.model.PurchaseInvoice;
import com.accounting.model.SaleInvoice;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InvoicePDFGenerator {

    private static final Logger log = AppLogger.get(InvoicePDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String IMAGES_FOLDER = "src/main/resources/images";

    public static void generatePurchaseInvoicePDF(PurchaseInvoice invoice, String outputPath) {
        generatePurchaseInvoicePDF(invoice, outputPath, "Original");
    }

    public static void generatePurchaseInvoicePDF(PurchaseInvoice invoice, String outputPath, String copyLabel) {
        try {
            float margin = 18;
            float contentWidth = PageSize.A4.getWidth() - 2 * margin;

            // Load images for page events
            Image headerImage = loadScaledImage("HEADER", contentWidth);
            Image footerImage = loadScaledImage("FOOTER", contentWidth);

            float headerHeight = headerImage != null ? headerImage.getScaledHeight() : 0;
            float footerHeight = footerImage != null ? footerImage.getScaledHeight() : 0;

            // Set margins to reserve space for header/footer images + line + padding
            float topMargin = headerImage != null ? margin + headerHeight + 8 : margin;
            float bottomMargin = footerImage != null ? 5 + footerHeight + 5 : margin;

            Document document = new Document(PageSize.A4, margin, margin, topMargin, bottomMargin);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));

            // Register page event for header/footer on every page
            writer.setPageEvent(new HeaderFooterEvent(headerImage, footerImage, headerHeight, footerHeight));

            document.open();

            // If no header image, add text-based header as content
            if (headerImage == null) {
                addTextBasedHeader(document);
            }

            // Add Original/Duplicate and Tax Invoice title
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);

            Paragraph origDup = new Paragraph(copyLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            origDup.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell origDupCell = new PdfPCell(origDup);
            origDupCell.setBorder(Rectangle.NO_BORDER);
            origDupCell.setPadding(8);
            origDupCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            titleTable.addCell(origDupCell);

            Paragraph title = new Paragraph("Tax Invoice", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE));
            title.setAlignment(Element.ALIGN_CENTER);
            PdfPCell titleCell = new PdfPCell(title);
            titleCell.setBackgroundColor(new Color(0, 51, 102));
            titleCell.setPadding(5);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleTable.addCell(titleCell);

            document.add(titleTable);

            // Add invoice details section (only on first page)
            addPurchaseInvoiceDetails(document, invoice);

            // Calculate available space and split line items across pages if needed
            float pageHeight = PageSize.A4.getHeight();
            float availableHeight = pageHeight - topMargin - bottomMargin;
            
            // Estimate heights for static content
            float invoiceDetailsHeight = 120; // Approximate height for invoice details
            float bankGSTHeight = 180; // Bank/GST/Others + GST Amount/Net Amount + Rupees + Remarks
            float termsSignatureHeight = 120; // Terms and signature section
            float computerNoteHeight = 25; // Computer generated note
            float staticFooterHeight = bankGSTHeight + termsSignatureHeight + computerNoteHeight;
            
            // Height available for line items on first page
            float firstPageLineItemHeight = availableHeight - invoiceDetailsHeight - staticFooterHeight - 20; // 20 for spacing
            
            // Height available for line items on subsequent pages (no invoice details)
            float subsequentPageLineItemHeight = availableHeight - staticFooterHeight - 20;
            
            // Keep pagination estimate aligned with table cell minimum heights below
            float lineItemRowHeight = 16;
            float lineItemHeaderHeight = 16;
            
            int maxItemsFirstPage = (int) ((firstPageLineItemHeight - lineItemHeaderHeight) / lineItemRowHeight);
            int maxItemsSubsequentPage = (int) ((subsequentPageLineItemHeight - lineItemHeaderHeight) / lineItemRowHeight);
            
            // Ensure at least 1 item per page
            maxItemsFirstPage = Math.max(1, maxItemsFirstPage);
            maxItemsSubsequentPage = Math.max(1, maxItemsSubsequentPage);
            
            java.util.List<InvoiceLineItem> allItems = invoice.getLineItems();
            
            if (allItems.size() <= maxItemsFirstPage) {
                // All items fit on first page
                addLineItemsTable(document, allItems);
                addBankAndGSTDetails(document, invoice);
                addTermsAndSignature(document);
                if (footerImage == null) {
                    addTextBasedFooter(document);
                }
            } else {
                // Need multiple pages
                // First page: invoice details + first chunk of items + static footer
                java.util.List<InvoiceLineItem> firstPageItems = allItems.subList(0, Math.min(maxItemsFirstPage, allItems.size()));
                addLineItemsTable(document, firstPageItems);
                addBankAndGSTDetails(document, invoice);
                addTermsAndSignature(document);
                if (footerImage == null) {
                    addTextBasedFooter(document);
                }
                
                // Subsequent pages: remaining items in chunks + static footer
                int remainingStart = maxItemsFirstPage;
                while (remainingStart < allItems.size()) {
                    document.newPage();
                    
                    // Add title on each page
                    PdfPTable titleTable2 = new PdfPTable(1);
                    titleTable2.setWidthPercentage(100);
                    Paragraph origDup2 = new Paragraph(copyLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
                    origDup2.setAlignment(Element.ALIGN_RIGHT);
                    PdfPCell origDupCell2 = new PdfPCell(origDup2);
                    origDupCell2.setBorder(Rectangle.NO_BORDER);
                    origDupCell2.setPadding(2);
                    origDupCell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    titleTable2.addCell(origDupCell2);
                    Paragraph title2 = new Paragraph("Tax Invoice (Continued)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE));
                    title2.setAlignment(Element.ALIGN_CENTER);
                    PdfPCell titleCell2 = new PdfPCell(title2);
                    titleCell2.setBackgroundColor(new Color(0, 51, 102));
                    titleCell2.setPadding(5);
                    titleCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    titleTable2.addCell(titleCell2);
                    document.add(titleTable2);
                    
                    int remainingEnd = Math.min(remainingStart + maxItemsSubsequentPage, allItems.size());
                    java.util.List<InvoiceLineItem> pageItems = allItems.subList(remainingStart, remainingEnd);
                    addLineItemsTable(document, pageItems);
                    
                    addBankAndGSTDetails(document, invoice);
                    addTermsAndSignature(document);
                    if (footerImage == null) {
                        addTextBasedFooter(document);
                    }
                    
                    remainingStart = remainingEnd;
                }
            }

            document.close();
            log.info("Purchase Invoice PDF generated successfully: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Purchase Invoice PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    public static void generateSaleInvoicePDF(SaleInvoice invoice, String outputPath) {
        generateSaleInvoicePDF(invoice, outputPath, "Original");
    }

    public static void generateSaleInvoicePDF(SaleInvoice invoice, String outputPath, String copyLabel) {
        try {
            float margin = 18;
            float contentWidth = PageSize.A4.getWidth() - 2 * margin;

            // Load images for page events
            Image headerImage = loadScaledImage("HEADER", contentWidth);
            Image footerImage = loadScaledImage("FOOTER", contentWidth);

            float headerHeight = headerImage != null ? headerImage.getScaledHeight() : 0;
            float footerHeight = footerImage != null ? footerImage.getScaledHeight() : 0;

            // Set margins to reserve space for header/footer images + line + padding
            float topMargin = headerImage != null ? margin + headerHeight + 8 : margin;
            float bottomMargin = footerImage != null ? 5 + footerHeight + 5 : margin;

            Document document = new Document(PageSize.A4, margin, margin, topMargin, bottomMargin);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));

            // Register page event for header/footer on every page
            writer.setPageEvent(new HeaderFooterEvent(headerImage, footerImage, headerHeight, footerHeight));

            document.open();

            // If no header image, add text-based header as content
            if (headerImage == null) {
                addTextBasedHeader(document);
            }

            // Add Original/Duplicate and Tax Invoice title
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);

            Paragraph origDup = new Paragraph(copyLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            origDup.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell origDupCell = new PdfPCell(origDup);
            origDupCell.setBorder(Rectangle.NO_BORDER);
            origDupCell.setPadding(8);
            origDupCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            titleTable.addCell(origDupCell);

            Paragraph title = new Paragraph("Tax Invoice", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE));
            title.setAlignment(Element.ALIGN_CENTER);
            PdfPCell titleCell = new PdfPCell(title);
            titleCell.setBackgroundColor(new Color(0, 51, 102));
            titleCell.setPadding(5);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleTable.addCell(titleCell);

            document.add(titleTable);

            // Add invoice details section (only on first page)
            addSaleInvoiceDetails(document, invoice);

            // Calculate available space and split line items across pages if needed
            float pageHeight = PageSize.A4.getHeight();
            float availableHeight = pageHeight - topMargin - bottomMargin;
            
            // Estimate heights for static content
            float invoiceDetailsHeight = 120; // Approximate height for invoice details
            float bankGSTHeight = 180; // Bank/GST/Others + GST Amount/Net Amount + Rupees + Remarks
            float termsSignatureHeight = 120; // Terms and signature section
            float computerNoteHeight = 25; // Computer generated note
            float staticFooterHeight = bankGSTHeight + termsSignatureHeight + computerNoteHeight;
            
            // Height available for line items on first page
            float firstPageLineItemHeight = availableHeight - invoiceDetailsHeight - staticFooterHeight - 20; // 20 for spacing
            
            // Height available for line items on subsequent pages (no invoice details)
            float subsequentPageLineItemHeight = availableHeight - staticFooterHeight - 20;
            
            // Keep pagination estimate aligned with table cell minimum heights below
            float lineItemRowHeight = 16;
            float lineItemHeaderHeight = 16;
            
            int maxItemsFirstPage = (int) ((firstPageLineItemHeight - lineItemHeaderHeight) / lineItemRowHeight);
            int maxItemsSubsequentPage = (int) ((subsequentPageLineItemHeight - lineItemHeaderHeight) / lineItemRowHeight);
            
            // Ensure at least 1 item per page
            maxItemsFirstPage = Math.max(1, maxItemsFirstPage);
            maxItemsSubsequentPage = Math.max(1, maxItemsSubsequentPage);
            
            java.util.List<InvoiceLineItem> allItems = invoice.getLineItems();
            
            if (allItems.size() <= maxItemsFirstPage) {
                // All items fit on first page
                addLineItemsTable(document, allItems);
                addBankAndGSTDetails(document, invoice);
                addTermsAndSignature(document);
                if (footerImage == null) {
                    addTextBasedFooter(document);
                }
            } else {
                // Need multiple pages
                // First page: invoice details + first chunk of items + static footer
                java.util.List<InvoiceLineItem> firstPageItems = allItems.subList(0, Math.min(maxItemsFirstPage, allItems.size()));
                addLineItemsTable(document, firstPageItems);
                addBankAndGSTDetails(document, invoice);
                addTermsAndSignature(document);
                if (footerImage == null) {
                    addTextBasedFooter(document);
                }
                
                // Subsequent pages: remaining items in chunks + static footer
                int remainingStart = maxItemsFirstPage;
                while (remainingStart < allItems.size()) {
                    document.newPage();
                    
                    // Add title on each page
                    PdfPTable titleTable2 = new PdfPTable(1);
                    titleTable2.setWidthPercentage(100);
                    Paragraph origDup2 = new Paragraph(copyLabel, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
                    origDup2.setAlignment(Element.ALIGN_RIGHT);
                    PdfPCell origDupCell2 = new PdfPCell(origDup2);
                    origDupCell2.setBorder(Rectangle.NO_BORDER);
                    origDupCell2.setPadding(2);
                    origDupCell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    titleTable2.addCell(origDupCell2);
                    Paragraph title2 = new Paragraph("Tax Invoice (Continued)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE));
                    title2.setAlignment(Element.ALIGN_CENTER);
                    PdfPCell titleCell2 = new PdfPCell(title2);
                    titleCell2.setBackgroundColor(new Color(0, 51, 102));
                    titleCell2.setPadding(5);
                    titleCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    titleTable2.addCell(titleCell2);
                    document.add(titleTable2);
                    
                    int remainingEnd = Math.min(remainingStart + maxItemsSubsequentPage, allItems.size());
                    java.util.List<InvoiceLineItem> pageItems = allItems.subList(remainingStart, remainingEnd);
                    addLineItemsTable(document, pageItems);
                    
                    addBankAndGSTDetails(document, invoice);
                    addTermsAndSignature(document);
                    if (footerImage == null) {
                        addTextBasedFooter(document);
                    }
                    
                    remainingStart = remainingEnd;
                }
            }

            document.close();
            log.info("Sale Invoice PDF generated successfully: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Sale Invoice PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private static void addHeader(Document document) throws DocumentException {
        try {
            // Try to load header image (check multiple extensions)
            File headerFile = findImageFile(IMAGES_FOLDER, "HEADER");
            
            if (headerFile.exists()) {
                Image headerImage = Image.getInstance(headerFile.getAbsolutePath());
                float cw = PageSize.A4.getWidth() - 36;
                headerImage.scaleAbsolute(cw, headerImage.getHeight() * cw / headerImage.getWidth());
                headerImage.setAlignment(Element.ALIGN_CENTER);
                document.add(headerImage);
            } else {
                // Fallback to text-based header if image not found
                log.warn("Header image not found. Using text-based header.");
                addTextBasedHeader(document);
            }
        } catch (Exception e) {
            log.error("Failed to load header image, using text-based header", e);
            addTextBasedHeader(document);
        }
    }

    private static Image loadScaledImage(String name, float contentWidth) {
        try {
            File imageFile = findImageFile(IMAGES_FOLDER, name);
            if (imageFile.exists()) {
                Image image = Image.getInstance(imageFile.getAbsolutePath());
                image.scaleAbsolute(contentWidth, image.getHeight() * contentWidth / image.getWidth());
                return image;
            }
        } catch (Exception e) {
            log.error("Failed to load image: " + name, e);
        }
        return null;
    }

    private static File findImageFile(String folder, String name) {
        // Check for various image extensions
        String[] extensions = {".png", ".PNG", ".jpg", ".JPG", ".jpeg", ".JPEG"};
        for (String ext : extensions) {
            File file = new File(folder + "/" + name + ext);
            if (file.exists()) {
                return file;
            }
        }
        return new File(folder + "/" + name + ".png"); // Return default path even if doesn't exist
    }

    private static void addTextBasedHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3, 2});

        // Logo placeholder
        PdfPCell logoCell = new PdfPCell(new Phrase("SE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, new Color(0, 51, 153))));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(logoCell);

        // Company name
        Paragraph companyName = new Paragraph();
        companyName.add(new Chunk("SIHAG ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(0, 51, 153))));
        companyName.add(new Chunk("ENTERPRISE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(204, 0, 0))));
        PdfPCell nameCell = new PdfPCell(companyName);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(nameCell);

        // PAN and GSTIN
        Paragraph panGstin = new Paragraph();
        panGstin.add(new Chunk("PAN : ", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        panGstin.add(new Chunk("DYHPD9230H\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(204, 0, 0))));
        panGstin.add(new Chunk("GSTIN : ", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        panGstin.add(new Chunk("24DYHPD9230H1ZJ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(204, 0, 0))));
        PdfPCell panGstinCell = new PdfPCell(panGstin);
        panGstinCell.setBorder(Rectangle.NO_BORDER);
        panGstinCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(panGstinCell);

        document.add(headerTable);

        // Add horizontal line
        LineSeparator line = new LineSeparator(1, 100, new Color(0, 51, 102), Element.ALIGN_CENTER, -2);
        document.add(new Chunk(line));
    }

    private static void addPurchaseInvoiceDetails(Document document, PurchaseInvoice invoice) throws DocumentException {
        // Invoice No and Date - 4 columns: label | value | label | value
        PdfPTable invoiceTable = new PdfPTable(4);
        invoiceTable.setWidthPercentage(100);
        invoiceTable.setWidths(new float[]{1.2f, 0.8f, 1.2f, 0.8f});
        invoiceTable.setSpacingBefore(5);

        invoiceTable.addCell(createCell("Invoice No -", true));
        invoiceTable.addCell(createCell(invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : "", false));
        invoiceTable.addCell(createCell("Invoice Date -", true));
        invoiceTable.addCell(createCell(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FORMATTER) : "", false));

        document.add(invoiceTable);

        // Billed To, Name, Address - single column
        PdfPTable billedToTable = new PdfPTable(1);
        billedToTable.setWidthPercentage(100);

        billedToTable.addCell(createCell("........Billed To..........", true));
        billedToTable.addCell(createCell("Name - " + (invoice.getPartyName() != null ? invoice.getPartyName() : ""), false));
        billedToTable.addCell(createCell("Address - " + (invoice.getSupplierAddress() != null ? invoice.getSupplierAddress() : ""), false));

        document.add(billedToTable);

        // GSTIN, PAN No, State Code - 5 columns with separate label/value
        PdfPTable gstTable = new PdfPTable(5);
        gstTable.setWidthPercentage(100);
        gstTable.setWidths(new float[]{2f, 0.7f, 1f, 0.8f, 0.5f});

        gstTable.addCell(createCell("GSTIN - " + (invoice.getSupplierGstNo() != null ? invoice.getSupplierGstNo() : ""), false));
        gstTable.addCell(createCell("PAN No", false));
        gstTable.addCell(createCell("", false));
        gstTable.addCell(createCell("State Code", false));
        gstTable.addCell(createCell("27", false));

        document.add(gstTable);

        // State and Contact - use same 5-col widths as GSTIN row for alignment
        PdfPTable stateContactTable = new PdfPTable(5);
        stateContactTable.setWidthPercentage(100);
        stateContactTable.setWidths(new float[]{2f, 0.7f, 1f, 0.8f, 0.5f});

        stateContactTable.addCell(createCell("State - MAHARASHTRA", false));
        PdfPCell contactCell = createCell("Contact No - " + (invoice.getSupplierContactNumber() != null ? invoice.getSupplierContactNumber() : ""), false);
        contactCell.setColspan(4);
        stateContactTable.addCell(contactCell);

        document.add(stateContactTable);
    }

    private static void addSaleInvoiceDetails(Document document, SaleInvoice invoice) throws DocumentException {
        // Invoice No and Date - 4 columns: label | value | label | value
        PdfPTable invoiceTable = new PdfPTable(4);
        invoiceTable.setWidthPercentage(100);
        invoiceTable.setWidths(new float[]{1.2f, 0.8f, 1.2f, 0.8f});
        invoiceTable.setSpacingBefore(5);

        invoiceTable.addCell(createCell("Invoice No -", true));
        invoiceTable.addCell(createCell(invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : "", false));
        invoiceTable.addCell(createCell("Invoice Date -", true));
        invoiceTable.addCell(createCell(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FORMATTER) : "", false));

        document.add(invoiceTable);

        // Billed To, Name, Address - single column
        PdfPTable billedToTable = new PdfPTable(1);
        billedToTable.setWidthPercentage(100);

        billedToTable.addCell(createCell("........Billed To..........", true));
        billedToTable.addCell(createCell("Name - " + (invoice.getPartyName() != null ? invoice.getPartyName() : ""), false));
        billedToTable.addCell(createCell("Address - " + (invoice.getRcvrAddress() != null ? invoice.getRcvrAddress() : ""), false));

        document.add(billedToTable);

        // GSTIN, PAN No, State Code - 5 columns with separate label/value
        PdfPTable gstTable = new PdfPTable(5);
        gstTable.setWidthPercentage(100);
        gstTable.setWidths(new float[]{2f, 0.7f, 1f, 0.8f, 0.5f});

        gstTable.addCell(createCell("GSTIN - " + (invoice.getRcvrGstin() != null ? invoice.getRcvrGstin() : ""), false));
        gstTable.addCell(createCell("PAN No", false));
        gstTable.addCell(createCell("", false));
        gstTable.addCell(createCell("State Code", false));
        gstTable.addCell(createCell("27", false));

        document.add(gstTable);

        // State and Contact - use same 5-col widths as GSTIN row for alignment
        PdfPTable stateContactTable = new PdfPTable(5);
        stateContactTable.setWidthPercentage(100);
        stateContactTable.setWidths(new float[]{2f, 0.7f, 1f, 0.8f, 0.5f});

        stateContactTable.addCell(createCell("State - MAHARASHTRA", false));
        PdfPCell contactCell = createCell("Contact No - " + (invoice.getRcvrContactNo() != null ? invoice.getRcvrContactNo() : ""), false);
        contactCell.setColspan(4);
        stateContactTable.addCell(contactCell);

        document.add(stateContactTable);
    }

    private static void addLineItemsTable(Document document, java.util.List<InvoiceLineItem> lineItems) throws DocumentException {
        PdfPTable table = new PdfPTable(12);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.4f, 0.85f, 0.75f, 1.15f, 1.15f, 1.35f, 1.35f, 0.75f, 0.55f, 0.55f, 0.55f, 0.6f});
        table.setSpacingBefore(8);

        // Header row (multi-line where needed so price columns stay narrow)
        addTableHeader(table, "Sr.");
        addTableHeader(table, "Date");
        addTableHeader(table, "LR\nNo");
        addTableHeader(table, "Container\nNo");
        addTableHeader(table, "Vehicle\nNo");
        addTableHeader(table, "From");
        addTableHeader(table, "To");
        addTableHeader(table, "Type");
        addTableHeader(table, "Basic\nFreight");
        addTableHeader(table, "Detention\nCharge");
        addTableHeader(table, "Other\nCharge");
        addTableHeader(table, "Total");

        // Data rows
        int srNo = 1;
        double totalAmount = 0;
        double totalOtherCharges = 0;
        for (InvoiceLineItem item : lineItems) {
            addTableCellCenter(table, String.valueOf(srNo++));
            addTableCellCenter(table, formatDateForPDF(item.getDate()));
            addTableCellCenter(table, item.getLrNo() != null ? item.getLrNo() : "");
            addTableCellCenter(table, item.getContainerNo() != null ? item.getContainerNo() : "");
            addTableCellCenter(table, item.getVehicleNo() != null ? item.getVehicleNo() : "");
            addTableCellCenter(table, item.getFrom() != null ? item.getFrom() : "");
            addTableCellCenter(table, item.getTo() != null ? item.getTo() : "");
            addTableCellCenter(table, item.getType() != null ? item.getType() : "");
            addTableCellRight(table, String.format("%.2f", item.getBasicFreight()));
            addTableCellRight(table, String.format("%.2f", item.getDetentionCharge()));
            addTableCellRight(table, String.format("%.2f", item.getOtherCharges()));
            addTableCellRight(table, String.format("%.2f", item.getTotal()));
            totalAmount += item.getTotal();
            totalOtherCharges += item.getOtherCharges();
        }

        // Total row at bottom right
        PdfPCell emptyCell = new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 7.2f)));
        emptyCell.setColspan(11);
        emptyCell.setBorder(Rectangle.BOX);
        emptyCell.setPadding(2.5f);
        emptyCell.setMinimumHeight(16f);
        table.addCell(emptyCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(String.format("%.2f", totalAmount), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f)));
        totalValueCell.setBorder(Rectangle.BOX);
        totalValueCell.setPadding(2.5f);
        totalValueCell.setMinimumHeight(16f);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private static void addBankAndGSTDetails(Document document, Object invoice) throws DocumentException {
        double taxableAmount = 0;
        double sgstAmount = 0;
        double cgstAmount = 0;
        double igstAmount = 0;
        double totalGst = 0;
        double netAmount = 0;

        if (invoice instanceof PurchaseInvoice) {
            PurchaseInvoice pi = (PurchaseInvoice) invoice;
            taxableAmount = pi.getTaxableAmount();
            sgstAmount = pi.getSgstAmount();
            cgstAmount = pi.getCgstAmount();
            igstAmount = pi.getIgstAmount();
            totalGst = pi.getTotalGst();
            netAmount = pi.getNetAmount();
        } else if (invoice instanceof SaleInvoice) {
            SaleInvoice si = (SaleInvoice) invoice;
            taxableAmount = si.getTaxableAmount();
            sgstAmount = si.getSgstAmount();
            cgstAmount = si.getCgstAmount();
            igstAmount = si.getIgstAmount();
            totalGst = si.getTotalGst();
            netAmount = si.getNetAmount();
        }

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Calculate total other charges from line items
        double totalOtherCharges = 0;
        if (invoice instanceof PurchaseInvoice) {
            for (InvoiceLineItem item : ((PurchaseInvoice) invoice).getLineItems()) {
                totalOtherCharges += item.getOtherCharges();
            }
        } else if (invoice instanceof SaleInvoice) {
            for (InvoiceLineItem item : ((SaleInvoice) invoice).getLineItems()) {
                totalOtherCharges += item.getOtherCharges();
            }
        }

        // 5-column row-based table: Bank | GST Label | GST Value | Others Label | Others Value
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 1.2f, 0.6f, 1.8f, 0.6f});
        table.setSpacingBefore(8);

        // Header row
        PdfPCell bankHeader = new PdfPCell(new Phrase("BANK DETAIL", headerFont));
        bankHeader.setBorder(Rectangle.BOX);
        bankHeader.setPadding(3);
        bankHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        bankHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(bankHeader);

        PdfPCell gstHeader = new PdfPCell(new Phrase("GST DETAIL", headerFont));
        gstHeader.setColspan(2);
        gstHeader.setBorder(Rectangle.BOX);
        gstHeader.setPadding(3);
        gstHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        gstHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(gstHeader);

        PdfPCell othersLabelHeader = new PdfPCell(new Phrase("Other Charges", headerFont));
        othersLabelHeader.setBorder(Rectangle.BOX);
        othersLabelHeader.setPadding(3);
        othersLabelHeader.setHorizontalAlignment(Element.ALIGN_LEFT);
        othersLabelHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(othersLabelHeader);

        PdfPCell othersValueHeader = new PdfPCell(new Phrase(String.format("%.2f", totalOtherCharges), normalFont));
        othersValueHeader.setBorder(Rectangle.BOX);
        othersValueHeader.setPadding(3);
        othersValueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        othersValueHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(othersValueHeader);

        // Row 1
        addBankGSTRow(table, "A/C NAME - SIHAG ENTERPRISE", false,
                "Taxable Amount", String.format("%.2f", taxableAmount),
                "Loading & Unloading Charges", "0.00");

        // Row 2
        addBankGSTRow(table, "Bank Detail - AXIS BANK,", false,
                "SGST 9%", String.format("%.2f", sgstAmount),
                "Weigh Bridge Charges", "0.00");

        // Row 3
        addBankGSTRow(table, "Branch - Mundra", false,
                "CGST 9%", String.format("%.2f", cgstAmount),
                "Taxable Amount", String.format("%.2f", taxableAmount));

        // Row 4
        addBankGSTRow(table, "Bank Account - 922020026748406", false,
                "IGST 18%", String.format("%.2f", igstAmount),
                "GST Amount", String.format("%.2f", totalGst));

        // Row 5
        addBankGSTRow(table, "IFSC Code - UTIB0000460", false,
                "Total GST", String.format("%.2f", totalGst),
                "Advance Amount", "0.00");

        document.add(table);

        // GST Amount and Net Amount row - use same 5-col widths as bank table for alignment
        PdfPTable gstNetTable = new PdfPTable(5);
        gstNetTable.setWidthPercentage(100);
        gstNetTable.setWidths(new float[]{2.2f, 1.2f, 0.6f, 1.8f, 0.6f});

        PdfPCell gstAmountCell = new PdfPCell(new Phrase("GST Amount - " + String.format("%.2f", totalGst), headerFont));
        gstAmountCell.setColspan(3);
        gstAmountCell.setBorder(Rectangle.BOX);
        gstAmountCell.setPadding(3);
        gstNetTable.addCell(gstAmountCell);

        PdfPCell netLabel = new PdfPCell(new Phrase("Net Amount", headerFont));
        netLabel.setBorder(Rectangle.BOX);
        netLabel.setPadding(3);
        netLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        gstNetTable.addCell(netLabel);

        PdfPCell netValue = new PdfPCell(new Phrase(String.format("%.2f", netAmount), headerFont));
        netValue.setBorder(Rectangle.BOX);
        netValue.setPadding(3);
        netValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        gstNetTable.addCell(netValue);

        document.add(gstNetTable);

        // Rupees in words - bordered
        PdfPTable rupeesTable = new PdfPTable(1);
        rupeesTable.setWidthPercentage(100);
        PdfPCell rupeesCell = new PdfPCell();
        rupeesCell.setBorder(Rectangle.BOX);
        rupeesCell.setPadding(3);
        Paragraph rupeesPara = new Paragraph();
        rupeesPara.add(new Chunk("Rupees - ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        rupeesPara.add(new Chunk(convertNumberToWords(netAmount) + " Only.", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        rupeesCell.addElement(rupeesPara);
        rupeesTable.addCell(rupeesCell);
        document.add(rupeesTable);

        // Remarks - always show with border
        String remarks = "";
        if (invoice instanceof PurchaseInvoice) {
            remarks = ((PurchaseInvoice) invoice).getRemarks();
        } else if (invoice instanceof SaleInvoice) {
            remarks = ((SaleInvoice) invoice).getRemarks();
        }
        PdfPTable remarksTable = new PdfPTable(1);
        remarksTable.setWidthPercentage(100);
        PdfPCell remarksCell = new PdfPCell(new Phrase("Remarks - " + (remarks != null ? remarks : ""), normalFont));
        remarksCell.setBorder(Rectangle.BOX);
        remarksCell.setPadding(3);
        remarksTable.addCell(remarksCell);
        document.add(remarksTable);
    }

    private static void addBankGSTRow(PdfPTable table, String bankText, boolean bankBold,
                                       String gstLabel, String gstValue,
                                       String othersLabel, String othersValue) {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        PdfPCell bankCell = new PdfPCell(new Phrase(bankText, bankBold ? boldFont : normalFont));
        bankCell.setBorder(Rectangle.BOX);
        bankCell.setPadding(3);
        bankCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(bankCell);

        PdfPCell gstLabelCell = new PdfPCell(new Phrase(gstLabel, normalFont));
        gstLabelCell.setBorder(Rectangle.BOX);
        gstLabelCell.setPadding(3);
        gstLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(gstLabelCell);

        PdfPCell gstValueCell = new PdfPCell(new Phrase(gstValue, normalFont));
        gstValueCell.setBorder(Rectangle.BOX);
        gstValueCell.setPadding(3);
        gstValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        gstValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(gstValueCell);

        PdfPCell othersLabelCell = new PdfPCell(new Phrase(othersLabel, normalFont));
        othersLabelCell.setBorder(Rectangle.BOX);
        othersLabelCell.setPadding(3);
        othersLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        othersLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(othersLabelCell);

        PdfPCell othersValueCell = new PdfPCell(new Phrase(othersValue, normalFont));
        othersValueCell.setBorder(Rectangle.BOX);
        othersValueCell.setPadding(3);
        othersValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        othersValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(othersValueCell);
    }

    private static String convertNumberToWords(double amount) {
        // Simple implementation for amount in words
        long rupees = (long) amount;
        long paise = (long) Math.round((amount - rupees) * 100);
        
        String words = convertToWords(rupees);
        if (paise > 0) {
            words += " and " + convertToWords(paise) + " Paise";
        }
        return words;
    }

    private static String convertToWords(long number) {
        if (number == 0) {
            return "Zero";
        }
        
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "Ten", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        
        if (number < 10) {
            return units[(int) number];
        } else if (number < 20) {
            return teens[(int) (number - 10)];
        } else if (number < 100) {
            return tens[(int) (number / 10)] + ((number % 10 != 0) ? " " + units[(int) (number % 10)] : "");
        } else if (number < 1000) {
            return units[(int) (number / 100)] + " Hundred" + ((number % 100 != 0) ? " " + convertToWords(number % 100) : "");
        } else if (number < 100000) {
            return convertToWords(number / 1000) + " Thousand" + ((number % 1000 != 0) ? " " + convertToWords(number % 1000) : "");
        } else if (number < 10000000) {
            return convertToWords(number / 100000) + " Lakh" + ((number % 100000 != 0) ? " " + convertToWords(number % 100000) : "");
        } else if (number < 1000000000) {
            return convertToWords(number / 10000000) + " Crore" + ((number % 10000000 != 0) ? " " + convertToWords(number % 10000000) : "");
        }
        return String.valueOf(number);
    }

    private static void addRemarks(Document document, String remarks) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell remarksCell = createCell("Remarks - " + remarks, false);
        table.addCell(remarksCell);

        document.add(table);
    }

    private static void addTermsAndSignature(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2});
        table.setSpacingBefore(10);

        // Terms and Condition
        PdfPCell termsCell = new PdfPCell();
        termsCell.setBorder(Rectangle.BOX);
        Paragraph termsPara = new Paragraph();
        termsPara.add(new Chunk("Terms and Condition\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        termsPara.add(new Chunk("1. Subject to Mundra jurisdiction\n", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        termsPara.add(new Chunk("2. In case of any correction in the bill, The same may be information with in\n", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        termsPara.add(new Chunk("07 Days of its Submission, after that no changes will be accepted\n", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        termsPara.add(new Chunk("3. Credit Limit 07-10 Days after Billing Date.\n", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        termsCell.addElement(termsPara);
        table.addCell(termsCell);

        // Signature
        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(Rectangle.BOX);
        
        // Try to load signature image
        Image signatureImage = loadScaledImage("SIGNATURE", 150); // Max width 150 for signature
        
        Paragraph signPara = new Paragraph();
        signPara.add(new Chunk("For SIHAG ENTERPRISE\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        
        if (signatureImage != null) {
            signPara.add(new Chunk("\n"));
            signatureCell.addElement(signPara);
            signatureImage.setAlignment(Element.ALIGN_CENTER);
            signatureCell.addElement(signatureImage);
            Paragraph authPara = new Paragraph("\nAuthorised Signatory", FontFactory.getFont(FontFactory.HELVETICA, 9));
            authPara.setAlignment(Element.ALIGN_CENTER);
            signatureCell.addElement(authPara);
        } else {
            signPara.add(new Chunk("\n\n\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            signPara.add(new Chunk("Authorised Signatory", FontFactory.getFont(FontFactory.HELVETICA, 9)));
            signPara.setAlignment(Element.ALIGN_CENTER);
            signatureCell.addElement(signPara);
        }
        
        signatureCell.setMinimumHeight(80);
        table.addCell(signatureCell);

        document.add(table);

        // Computer generated invoice note
        Paragraph note = new Paragraph("This is a Computer Generated Invoice\nAll Subject To MUNDRA Jurisdiction Only", 
            FontFactory.getFont(FontFactory.HELVETICA, 8));
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(5);
        document.add(note);
    }

    private static void addFooter(Document document) throws DocumentException {
        try {
            // Try to load footer image (check multiple extensions)
            File footerFile = findImageFile(IMAGES_FOLDER, "FOOTER");
            
            if (footerFile.exists()) {
                Image footerImage = Image.getInstance(footerFile.getAbsolutePath());
                // Scale image to fit page width (A4 width is 595, minus margins 36 on each side = 523)
                float cw2 = PageSize.A4.getWidth() - 36;
                footerImage.scaleAbsolute(cw2, footerImage.getHeight() * cw2 / footerImage.getWidth());
                footerImage.setAlignment(Element.ALIGN_CENTER);
                document.add(footerImage);
            } else {
                // Fallback to text-based footer if image not found
                log.warn("Footer image not found. Using text-based footer.");
                addTextBasedFooter(document);
            }
        } catch (Exception e) {
            log.error("Failed to load footer image, using text-based footer", e);
            addTextBasedFooter(document);
        }
    }

    private static void addTextBasedFooter(Document document) throws DocumentException {
        // Location marker
        Paragraph location = new Paragraph();
        location.add(new Chunk("📍", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(204, 0, 0))));
        location.add(new Chunk(" Shop No : 03, Commercial Zone, Survey No.33/1, Plot No-1, Nr. Shahid Manshi Circle\n", 
            FontFactory.getFont(FontFactory.HELVETICA, 9)));
        location.add(new Chunk("   Mundra Port Main Road, Nana Kapaya, Mundra-Kutch (Gujarat) 370 421", 
            FontFactory.getFont(FontFactory.HELVETICA, 9)));
        location.setAlignment(Element.ALIGN_CENTER);
        document.add(location);

        // Contact details
        Paragraph contact = new Paragraph();
        contact.add(new Chunk("📧", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0, 51, 153))));
        contact.add(new Chunk(" Email : sihag.enterprise23@gmail.com     ", 
            FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0, 51, 153))));
        contact.add(new Chunk("📞", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(204, 0, 0))));
        contact.add(new Chunk(" 9033 777 516 | 94260 36076", 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(204, 0, 0))));
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(5);
        document.add(contact);
    }

    private static void addDetailCell(PdfPTable table, String label, String value, boolean bold) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10) : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(label + " " + value, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setNoWrap(false); // Enable text wrapping
        table.addCell(cell);
    }

    private static PdfPCell createCell(String text, boolean bold) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10) : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setNoWrap(false); // Enable text wrapping
        return cell;
    }

    private static void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f)));
        cell.setBackgroundColor(new Color(220, 220, 220));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2.5f);
        cell.setMinimumHeight(16f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addTableCellCenter(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 7.2f)));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2.5f);
        cell.setMinimumHeight(16f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(false);
        table.addCell(cell);
    }

    private static void addTableCellRight(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 7.2f)));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2.5f);
        cell.setMinimumHeight(16f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(false);
        table.addCell(cell);
    }

    private static String formatDateForPDF(String date) {
        if (date == null || date.isEmpty()) return "";
        try {
            // Try parsing ISO format (yyyy-MM-dd)
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate ld = LocalDate.parse(date);
                return ld.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
            // Try parsing dd.MM.yyyy format
            if (date.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                LocalDate ld = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                return ld.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
        } catch (Exception ignored) {}
        return date;
    }

    private static void addGSTRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        labelCell.setNoWrap(false); // Enable text wrapping
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setNoWrap(false); // Enable text wrapping
        table.addCell(valueCell);
    }

    // Page event handler to draw header and footer images on every page
    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final Image headerImage;
        private final Image footerImage;
        private final float headerHeight;
        private final float footerHeight;

        public HeaderFooterEvent(Image headerImage, Image footerImage, float headerHeight, float footerHeight) {
            this.headerImage = headerImage;
            this.footerImage = footerImage;
            this.headerHeight = headerHeight;
            this.footerHeight = footerHeight;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float pageWidth = document.getPageSize().getWidth();
            float pageHeight = document.getPageSize().getHeight();
            float margin = 18;
            float contentWidth = pageWidth - 2 * margin;
            PdfContentByte cb = writer.getDirectContent();

            try {
                // Draw header image at top of every page
                if (headerImage != null) {
                    headerImage.setAbsolutePosition(margin, pageHeight - margin - headerHeight + 5);
                    cb.addImage(headerImage);

                    // Draw horizontal line below header
                    float lineY = pageHeight - margin - headerHeight + 2;
                    cb.setColorStroke(new Color(0, 51, 102));
                    cb.setLineWidth(1);
                    cb.moveTo(margin, lineY);
                    cb.lineTo(pageWidth - margin, lineY);
                    cb.stroke();
                }

                // Draw footer image at bottom of every page
                if (footerImage != null) {
                    footerImage.setAbsolutePosition(margin, 5);
                    cb.addImage(footerImage);
                }
            } catch (Exception e) {
                log.error("Failed to draw header/footer on page", e);
            }
        }
    }
}
