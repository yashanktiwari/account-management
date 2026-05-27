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
        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Add header
            addHeader(document);

            // Add horizontal line after header
            LineSeparator headerLine = new LineSeparator(1, 100, new Color(0, 51, 102), Element.ALIGN_CENTER, -2);
            document.add(new Chunk(headerLine));
            document.add(new Paragraph("\n")); // Add spacing after line

            // Add Original/Duplicate and Tax Invoice title
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);

            Paragraph origDup = new Paragraph("Original / Duplicate", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            origDup.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell origDupCell = new PdfPCell(origDup);
            origDupCell.setBorder(Rectangle.NO_BORDER);
            origDupCell.setPadding(2);
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

            // Add invoice details section
            addPurchaseInvoiceDetails(document, invoice);

            // Add line items table
            addLineItemsTable(document, invoice.getLineItems());

            // Add bank and GST details
            addBankAndGSTDetails(document, invoice);

            // Add terms and signature
            addTermsAndSignature(document);

            // Add footer at the bottom
            document.add(new Paragraph("\n\n")); // Reduced spacing to fit on one page
            addFooter(document);

            document.close();
            log.info("Purchase Invoice PDF generated successfully: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Purchase Invoice PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    public static void generateSaleInvoicePDF(SaleInvoice invoice, String outputPath) {
        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Add header
            addHeader(document);

            // Add horizontal line after header
            LineSeparator headerLine = new LineSeparator(1, 100, new Color(0, 51, 102), Element.ALIGN_CENTER, -2);
            document.add(new Chunk(headerLine));
            document.add(new Paragraph("\n")); // Add spacing after line

            // Add Original/Duplicate and Tax Invoice title
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);

            Paragraph origDup = new Paragraph("Original / Duplicate", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));
            origDup.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell origDupCell = new PdfPCell(origDup);
            origDupCell.setBorder(Rectangle.NO_BORDER);
            origDupCell.setPadding(2);
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

            // Add invoice details section
            addSaleInvoiceDetails(document, invoice);

            // Add line items table
            addLineItemsTable(document, invoice.getLineItems());

            // Add bank and GST details
            addBankAndGSTDetails(document, invoice);

            // Add terms and signature
            addTermsAndSignature(document);

            // Add footer at the bottom
            document.add(new Paragraph("\n\n")); // Reduced spacing to fit on one page
            addFooter(document);

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
                // Scale image to fit page width (A4 width is 595, minus margins 36 on each side = 523)
                headerImage.scaleAbsolute(523, headerImage.getHeight() * 523 / headerImage.getWidth());
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
        // Invoice No and Date section with border
        PdfPTable invoiceTable = new PdfPTable(2);
        invoiceTable.setWidthPercentage(100);
        invoiceTable.setWidths(new float[]{1f, 1f});
        invoiceTable.setSpacingBefore(10);

        addDetailCell(invoiceTable, "Invoice No -", invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : "", true);
        addDetailCell(invoiceTable, "Invoice Date -", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FORMATTER) : "", true);

        document.add(invoiceTable);

        // Billed To section - separate table with border
        PdfPTable billedToTable = new PdfPTable(1);
        billedToTable.setWidthPercentage(100);
        billedToTable.setSpacingBefore(5);

        PdfPCell billedToLabel = createCell("........Billed To..........", true);
        billedToTable.addCell(billedToLabel);

        PdfPCell supplierName = createCell("Name - " + (invoice.getPartyName() != null ? invoice.getPartyName() : ""), false);
        billedToTable.addCell(supplierName);

        PdfPCell supplierAddress = createCell("Address - " + (invoice.getSupplierAddress() != null ? invoice.getSupplierAddress() : ""), false);
        billedToTable.addCell(supplierAddress);

        document.add(billedToTable);

        // GSTIN, PAN, State Code in one line with border
        PdfPTable gstTable = new PdfPTable(3);
        gstTable.setWidthPercentage(100);
        gstTable.setWidths(new float[]{2f, 1.5f, 1f});
        gstTable.setSpacingBefore(5);

        addDetailCell(gstTable, "GSTIN -", invoice.getSupplierGstNo() != null ? invoice.getSupplierGstNo() : "", false);
        addDetailCell(gstTable, "PAN No", "", false);
        addDetailCell(gstTable, "State Code", "27", false);

        document.add(gstTable);

        // State and Contact in next line with border
        PdfPTable stateContactTable = new PdfPTable(2);
        stateContactTable.setWidthPercentage(100);
        stateContactTable.setWidths(new float[]{2f, 2f});
        stateContactTable.setSpacingBefore(0);

        addDetailCell(stateContactTable, "State -", "MAHARASHTRA", false);
        addDetailCell(stateContactTable, "Contact No -", invoice.getSupplierContactNumber() != null ? invoice.getSupplierContactNumber() : "", false);

        document.add(stateContactTable);
    }

    private static void addSaleInvoiceDetails(Document document, SaleInvoice invoice) throws DocumentException {
        // Invoice No and Date section with border
        PdfPTable invoiceTable = new PdfPTable(2);
        invoiceTable.setWidthPercentage(100);
        invoiceTable.setWidths(new float[]{1f, 1f});
        invoiceTable.setSpacingBefore(10);

        addDetailCell(invoiceTable, "Invoice No -", invoice.getInvoiceNo() != null ? invoice.getInvoiceNo() : "", true);
        addDetailCell(invoiceTable, "Invoice Date -", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FORMATTER) : "", true);

        document.add(invoiceTable);

        // Billed To section - separate table with border
        PdfPTable billedToTable = new PdfPTable(1);
        billedToTable.setWidthPercentage(100);
        billedToTable.setSpacingBefore(5);

        PdfPCell billedToLabel = createCell("........Billed To..........", true);
        billedToTable.addCell(billedToLabel);

        PdfPCell customerName = createCell("Name - " + (invoice.getPartyName() != null ? invoice.getPartyName() : ""), false);
        billedToTable.addCell(customerName);

        PdfPCell customerAddress = createCell("Address - " + (invoice.getRcvrAddress() != null ? invoice.getRcvrAddress() : ""), false);
        billedToTable.addCell(customerAddress);

        document.add(billedToTable);

        // GSTIN, PAN, State Code in one line with border
        PdfPTable gstTable = new PdfPTable(3);
        gstTable.setWidthPercentage(100);
        gstTable.setWidths(new float[]{2f, 1.5f, 1f});
        gstTable.setSpacingBefore(5);

        addDetailCell(gstTable, "GSTIN -", invoice.getRcvrGstin() != null ? invoice.getRcvrGstin() : "", false);
        addDetailCell(gstTable, "PAN No", "", false);
        addDetailCell(gstTable, "State Code", "27", false);

        document.add(gstTable);

        // State and Contact in next line with border
        PdfPTable stateContactTable = new PdfPTable(2);
        stateContactTable.setWidthPercentage(100);
        stateContactTable.setWidths(new float[]{2f, 2f});
        stateContactTable.setSpacingBefore(0);

        addDetailCell(stateContactTable, "State -", "MAHARASHTRA", false);
        addDetailCell(stateContactTable, "Contact No -", invoice.getRcvrContactNo() != null ? invoice.getRcvrContactNo() : "", false);

        document.add(stateContactTable);
    }

    private static void addLineItemsTable(Document document, java.util.List<InvoiceLineItem> lineItems) throws DocumentException {
        PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.4f, 0.8f, 0.8f, 1.0f, 1.0f, 0.8f, 0.8f, 0.8f, 1.0f, 1.0f, 1.0f});
        table.setSpacingBefore(10);

        // Header row
        addTableHeader(table, "Sr. No");
        addTableHeader(table, "Date");
        addTableHeader(table, "LR No");
        addTableHeader(table, "Container No");
        addTableHeader(table, "Vehicle No");
        addTableHeader(table, "From");
        addTableHeader(table, "To");
        addTableHeader(table, "Type");
        addTableHeader(table, "Basic Freight");
        addTableHeader(table, "Detention Charge");
        addTableHeader(table, "Total");

        // Data rows
        int srNo = 1;
        double totalAmount = 0;
        for (InvoiceLineItem item : lineItems) {
            addTableCell(table, String.valueOf(srNo++));
            addTableCell(table, item.getDate() != null ? item.getDate() : "");
            addTableCell(table, item.getLrNo() != null ? item.getLrNo() : "");
            addTableCell(table, item.getContainerNo() != null ? item.getContainerNo() : "");
            addTableCell(table, item.getVehicleNo() != null ? item.getVehicleNo() : "");
            addTableCell(table, item.getFrom() != null ? item.getFrom() : "");
            addTableCell(table, item.getTo() != null ? item.getTo() : "");
            addTableCell(table, item.getType() != null ? item.getType() : "");
            addTableCell(table, String.format("%.2f", item.getBasicFreight()));
            addTableCell(table, String.format("%.2f", item.getDetentionCharge()));
            addTableCell(table, String.format("%.2f", item.getTotal()));
            totalAmount += item.getTotal();
        }

        document.add(table);
    }

    private static void addBankAndGSTDetails(Document document, Object invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 2, 2});
        table.setSpacingBefore(10);

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

        // Bank Details Column
        PdfPCell bankHeader = createCell("BANK DETAIL", true);
        bankHeader.setBackgroundColor(new Color(220, 220, 220));
        table.addCell(bankHeader);

        // GST Details Column
        PdfPCell gstHeader = createCell("GST DETAIL", true);
        gstHeader.setBackgroundColor(new Color(220, 220, 220));
        table.addCell(gstHeader);

        // Others Charge Column
        PdfPCell othersHeader = createCell("Others Charge", true);
        othersHeader.setBackgroundColor(new Color(220, 220, 220));
        table.addCell(othersHeader);

        // Bank details
        PdfPCell bankDetails = new PdfPCell();
        bankDetails.setBorder(Rectangle.BOX);
        Paragraph bankPara = new Paragraph();
        bankPara.add(new Chunk("A/C NAME - SIHAG ENTERPRISE\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        bankPara.add(new Chunk("Bank Detail - AXIS BANK,\n", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        bankPara.add(new Chunk("Branch - Mundra\n", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        bankPara.add(new Chunk("Bank Account - 922020026748406\n", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        bankPara.add(new Chunk("IFSC Code - UTIB0000460\n", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        bankDetails.addElement(bankPara);
        table.addCell(bankDetails);

        // GST details
        PdfPCell gstDetails = new PdfPCell();
        gstDetails.setBorder(Rectangle.BOX);
        PdfPTable gstTable = new PdfPTable(2);
        gstTable.setWidthPercentage(100);
        
        addGSTRow(gstTable, "Taxable Amount", String.format("%.2f", taxableAmount));
        if (sgstAmount > 0) {
            addGSTRow(gstTable, "SGST 9%", String.format("%.2f", sgstAmount));
        }
        if (cgstAmount > 0) {
            addGSTRow(gstTable, "CGST 9%", String.format("%.2f", cgstAmount));
        }
        if (igstAmount > 0) {
            addGSTRow(gstTable, "IGST 18%", String.format("%.2f", igstAmount));
        }
        addGSTRow(gstTable, "Total GST", String.format("%.2f", totalGst));
        
        gstDetails.addElement(gstTable);
        table.addCell(gstDetails);

        // Others charge
        PdfPCell othersDetails = new PdfPCell();
        othersDetails.setBorder(Rectangle.BOX);
        PdfPTable othersTable = new PdfPTable(2);
        othersTable.setWidthPercentage(100);
        
        addGSTRow(othersTable, "Loading & Unloading Charges", "0.00");
        addGSTRow(othersTable, "Weigh Bridge Charges", "0.00");
        addGSTRow(othersTable, "Taxable Amount", String.format("%.2f", taxableAmount));
        addGSTRow(othersTable, "GST Amount", String.format("%.2f", totalGst));
        addGSTRow(othersTable, "Advance Amount", "0.00");
        
        othersDetails.addElement(othersTable);
        table.addCell(othersDetails);

        document.add(table);

        // GST Amount and Net Amount row - separate table below bank details
        PdfPTable gstNetTable = new PdfPTable(2);
        gstNetTable.setWidthPercentage(100);
        gstNetTable.setWidths(new float[]{1f, 1f});
        gstNetTable.setSpacingBefore(5);

        PdfPCell gstAmountLabel = createCell("GST Amount - " + String.format("%.2f", totalGst), true);
        gstAmountLabel.setBorder(Rectangle.BOX);
        gstNetTable.addCell(gstAmountLabel);

        PdfPCell netAmountCell = createCell("Net Amount - " + String.format("%.2f", netAmount), true);
        netAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netAmountCell.setBorder(Rectangle.BOX);
        gstNetTable.addCell(netAmountCell);

        document.add(gstNetTable);

        // Amount in words
        Paragraph amountWords = new Paragraph();
        amountWords.add(new Chunk("Rupees - ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        amountWords.add(new Chunk(convertNumberToWords(netAmount) + " Only", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        amountWords.setSpacingBefore(10);
        amountWords.setAlignment(Element.ALIGN_LEFT);
        document.add(amountWords);

        // Remarks section
        String remarks = "";
        if (invoice instanceof PurchaseInvoice) {
            remarks = ((PurchaseInvoice) invoice).getRemarks();
        } else if (invoice instanceof SaleInvoice) {
            remarks = ((SaleInvoice) invoice).getRemarks();
        }
        
        if (remarks != null && !remarks.isEmpty()) {
            addRemarks(document, remarks);
        }
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
        Paragraph signPara = new Paragraph();
        signPara.add(new Chunk("For SIHAG ENTERPRISE\n\n\n\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        signPara.add(new Chunk("Authorised Signatory", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        signPara.setAlignment(Element.ALIGN_CENTER);
        signatureCell.addElement(signPara);
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
                footerImage.scaleAbsolute(523, footerImage.getHeight() * 523 / footerImage.getWidth());
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
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setBackgroundColor(new Color(220, 220, 220));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 7)));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setNoWrap(false); // Enable text wrapping
        table.addCell(cell);
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
}
