package com.accounting.util;

import com.accounting.model.LoadingSlip;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

public class LoadingSlipPDFGenerator {

    private static final Logger log = AppLogger.get(LoadingSlipPDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String IMAGES_FOLDER = "src/main/resources/images";

    public static void generateLoadingSlipPDF(LoadingSlip slip, String outputPath, String copyLabel) {
        try {
            float margin = 18;
            float contentWidth = PageSize.A4.getWidth() - 2 * margin;

            // Load images for page events
            Image headerImage = loadScaledImage("HEADER", contentWidth);
            Image footerImage = loadScaledImage("FOOTER", contentWidth);

            float headerHeight = headerImage != null ? headerImage.getScaledHeight() : 0;
            float footerHeight = footerImage != null ? footerImage.getScaledHeight() : 0;

            float topMargin = headerImage != null ? margin + headerHeight + 4 : margin;
            float bottomMargin = footerImage != null ? footerHeight + 10 : margin;

            Document document = new Document(PageSize.A4, margin, margin, topMargin, bottomMargin);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));

            writer.setPageEvent(new HeaderFooterEvent(headerImage, footerImage, headerHeight, footerHeight));

            document.open();

            if (headerImage == null) {
                addTextBasedHeader(document);
            }

            // ── LOADING SLIP title ──
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            titleTable.setSpacingBefore(4);

            Paragraph title = new Paragraph("LOADING SLIP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            title.setAlignment(Element.ALIGN_CENTER);
            PdfPCell titleCell = new PdfPCell(title);
            titleCell.setBorder(Rectangle.BOTTOM);
            titleCell.setBorderWidth(1.5f);
            titleCell.setBorderColor(new Color(0, 51, 102));
            titleCell.setPadding(6);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleTable.addCell(titleCell);

            document.add(titleTable);

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font largeBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            // ── Row: Slip No | Date ──
            PdfPTable row1 = new PdfPTable(4);
            row1.setWidthPercentage(100);
            row1.setWidths(new float[]{1.2f, 1.3f, 1.0f, 1.5f});
            row1.setSpacingBefore(8);

            row1.addCell(cellLabel("Slip No -", labelFont));
            row1.addCell(cellValue(slip.getSlipNo() != null ? slip.getSlipNo() : "", largeBoldFont));
            row1.addCell(cellLabel("Date -", labelFont));
            row1.addCell(cellValue(slip.getSlipDate() != null ? slip.getSlipDate().format(DATE_FORMATTER) : "", largeBoldFont));

            document.add(row1);

            // ── Party Name ──
            PdfPTable row2 = new PdfPTable(2);
            row2.setWidthPercentage(100);
            row2.setWidths(new float[]{1.2f, 3.8f});

            row2.addCell(cellLabel("Party Name -", labelFont));
            row2.addCell(cellValue(slip.getPartyName() != null ? slip.getPartyName() : "", largeBoldFont));

            document.add(row2);

            // ── Vehicle No | G.R. No ──
            PdfPTable row3 = new PdfPTable(4);
            row3.setWidthPercentage(100);
            row3.setWidths(new float[]{1.2f, 1.3f, 1.0f, 1.5f});

            row3.addCell(cellLabel("Vehicle No -", labelFont));
            row3.addCell(cellValue(slip.getVehicleNo() != null ? slip.getVehicleNo() : "", largeBoldFont));
            row3.addCell(cellLabel("G.R. No", labelFont));
            row3.addCell(cellValue(slip.getGrNo() != null ? slip.getGrNo() : "", valueFont));

            document.add(row3);

            // ── Station | To ──
            PdfPTable row4 = new PdfPTable(4);
            row4.setWidthPercentage(100);
            row4.setWidths(new float[]{1.2f, 1.3f, 1.0f, 1.5f});

            row4.addCell(cellLabel("Station", labelFont));
            row4.addCell(cellValue(slip.getStation() != null ? slip.getStation() : "", largeBoldFont));
            row4.addCell(cellLabel("To", labelFont));
            row4.addCell(cellValue(slip.getToLocation() != null ? slip.getToLocation() : "", largeBoldFont));

            document.add(row4);

            // ── Weight | Rate ──
            PdfPTable row5 = new PdfPTable(4);
            row5.setWidthPercentage(100);
            row5.setWidths(new float[]{1.2f, 1.3f, 1.0f, 1.5f});

            row5.addCell(cellLabel("Weight", labelFont));
            row5.addCell(cellValue(slip.getWeight() != null ? slip.getWeight() : "", largeBoldFont));
            row5.addCell(cellLabel("Rate", labelFont));
            row5.addCell(cellValue(slip.getRate() != null ? slip.getRate() : "", valueFont));

            document.add(row5);

            // ── Spacer ──
            document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            // ── Freight Amount ──
            PdfPTable freightRow = new PdfPTable(1);
            freightRow.setWidthPercentage(100);
            freightRow.addCell(cellFullWidth("Freight Amount - Rs. " + String.format("%.2f", slip.getFreightAmount()), largeBoldFont));
            document.add(freightRow);

            // ── Advance Amount ──
            PdfPTable advanceRow = new PdfPTable(1);
            advanceRow.setWidthPercentage(100);
            advanceRow.addCell(cellFullWidth("Advance Amount - Rs. " + String.format("%.2f", slip.getAdvanceAmount()), largeBoldFont));
            document.add(advanceRow);

            // ── Balance Amount ──
            PdfPTable balanceRow = new PdfPTable(1);
            balanceRow.setWidthPercentage(100);
            balanceRow.addCell(cellFullWidth("Balance Amount - Rs. " + String.format("%.2f", slip.getBalanceAmount()), largeBoldFont));
            document.add(balanceRow);

            // ── Bank Details + Signature ──
            PdfPTable bankSigTable = new PdfPTable(2);
            bankSigTable.setWidthPercentage(100);
            bankSigTable.setWidths(new float[]{3, 2});
            bankSigTable.setSpacingBefore(6);

            // Bank details cell
            PdfPCell bankCell = new PdfPCell();
            bankCell.setBorder(Rectangle.BOX);
            bankCell.setPadding(5);

            Paragraph bankPara = new Paragraph();
            bankPara.add(new Chunk("Bank - ", labelFont));
            bankPara.add(new Chunk(slip.getBankName() != null ? slip.getBankName() + " -" : "", largeBoldFont));
            bankCell.addElement(bankPara);

            bankCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

            Paragraph acPara = new Paragraph();
            acPara.add(new Chunk("A/C No - ", labelFont));
            acPara.add(new Chunk(slip.getAccountNo() != null ? slip.getAccountNo() : "", largeBoldFont));
            bankCell.addElement(acPara);

            bankCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

            Paragraph ifscPara = new Paragraph();
            ifscPara.add(new Chunk("IFSC - ", labelFont));
            ifscPara.add(new Chunk(slip.getIfscCode() != null ? slip.getIfscCode() : "", largeBoldFont));
            bankCell.addElement(ifscPara);

            bankCell.setMinimumHeight(90);
            bankSigTable.addCell(bankCell);

            // Signature cell
            PdfPCell signatureCell = new PdfPCell();
            signatureCell.setBorder(Rectangle.BOX);

            // Create a nested table for the signature area to allow overlapping
            PdfPTable sigTable = new PdfPTable(1);
            sigTable.setWidthPercentage(100);

            // Add AUTH_SIGN.png image (bottom layer)
            float sigCellWidth = (PageSize.A4.getWidth() - 36f) * 2f / 5f - 8f;
            float sigCellHeight = 50f;
            Image authSignImg = loadImageFitToBox("AUTH_SIGN", sigCellWidth, sigCellHeight);
            if (authSignImg != null) {
                PdfPCell authCell = new PdfPCell(authSignImg);
                authCell.setBorder(Rectangle.NO_BORDER);
                authCell.setPadding(0);
                authCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                sigTable.addCell(authCell);
            }

            // Add SIGNATURE.png image (top layer, overlapping AUTH_SIGN)
            Image signatureImg = loadImageFitToBox("SIGNATURE", sigCellWidth, sigCellHeight);
            if (signatureImg != null) {
                PdfPCell sigCell = new PdfPCell(signatureImg);
                sigCell.setBorder(Rectangle.NO_BORDER);
                sigCell.setPadding(0);
                sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                // Use negative top padding to overlap with the image below
                sigCell.setPaddingTop(-25f);
                sigTable.addCell(sigCell);
            }

            // Add "For SIHAG ENTERPRISE" text (without comma)
            Paragraph signPara = new Paragraph("For SIHAG ENTERPRISE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.ITALIC));
            signPara.setAlignment(Element.ALIGN_CENTER);
            signPara.setSpacingAfter(5f);
            PdfPCell textCell = new PdfPCell(signPara);
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setPadding(0);
            textCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigTable.addCell(textCell);

            signatureCell.addElement(sigTable);

            if (authSignImg != null || signatureImg != null) {
                Paragraph authPara = new Paragraph("Authorised Signatory", FontFactory.getFont(FontFactory.HELVETICA, 9));
                authPara.setAlignment(Element.ALIGN_CENTER);
                signatureCell.addElement(authPara);
            } else {
                Paragraph authPara = new Paragraph("\n\n\nAuthorised Signatory", FontFactory.getFont(FontFactory.HELVETICA, 9));
                authPara.setAlignment(Element.ALIGN_CENTER);
                signatureCell.addElement(authPara);
            }

            signatureCell.setMinimumHeight(90);
            bankSigTable.addCell(signatureCell);

            document.add(bankSigTable);

            if (footerImage == null) {
                addTextBasedFooter(document);
            }

            document.close();
            log.info("Loading Slip PDF generated successfully: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Loading Slip PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell cellLabel(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell cellValue(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell cellFullWidth(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static Image loadScaledImage(String name, float contentWidth) {
        try {
            String resourcePath = findResourcePath(name);
            if (resourcePath != null) {
                InputStream is = LoadingSlipPDFGenerator.class.getResourceAsStream(resourcePath);
                if (is != null) {
                    byte[] imageBytes = is.readAllBytes();
                    Image image = Image.getInstance(imageBytes);
                    image.scaleAbsolute(contentWidth, image.getHeight() * contentWidth / image.getWidth());
                    return image;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load image: " + name, e);
        }
        return null;
    }

    private static Image loadImageFitToBox(String name, float maxWidth, float maxHeight) {
        try {
            String resourcePath = findResourcePath(name);
            if (resourcePath != null) {
                InputStream is = LoadingSlipPDFGenerator.class.getResourceAsStream(resourcePath);
                if (is != null) {
                    byte[] imageBytes = is.readAllBytes();
                    Image image = Image.getInstance(imageBytes);
                    float w = image.getWidth();
                    float h = image.getHeight();
                    float scale = Math.min(maxWidth / w, maxHeight / h);
                    image.scaleAbsolute(w * scale, h * scale);
                    return image;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load image: " + name, e);
        }
        return null;
    }

    private static String findResourcePath(String name) {
        String[] extensions = {".png", ".PNG", ".jpg", ".JPG", ".jpeg", ".JPEG"};
        for (String ext : extensions) {
            String path = "/images/" + name + ext;
            if (LoadingSlipPDFGenerator.class.getResource(path) != null) {
                return path;
            }
        }
        return null;
    }

    private static void addTextBasedHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3, 2});

        PdfPCell logoCell = new PdfPCell(new Phrase("SE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, new Color(0, 51, 153))));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(logoCell);

        Paragraph companyName = new Paragraph();
        companyName.add(new Chunk("SIHAG ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(0, 51, 153))));
        companyName.add(new Chunk("ENTERPRISE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(204, 0, 0))));
        PdfPCell nameCell = new PdfPCell(companyName);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(nameCell);

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

        com.lowagie.text.pdf.draw.LineSeparator line = new com.lowagie.text.pdf.draw.LineSeparator(1, 100, new Color(0, 51, 102), Element.ALIGN_CENTER, -2);
        document.add(new Chunk(line));
    }

    private static void addTextBasedFooter(Document document) throws DocumentException {
        Paragraph location = new Paragraph();
        location.add(new Chunk(" Shop No : 03, Commercial Zone, Survey No.33/1, Plot No-1, Nr. Shahid Manshi Circle\n",
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        location.add(new Chunk("   Mundra Port Main Road, Nana Kapaya, Mundra-Kutch (Gujarat) 370 421",
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        location.setAlignment(Element.ALIGN_CENTER);
        location.setSpacingBefore(10);
        document.add(location);

        Paragraph contact = new Paragraph();
        contact.add(new Chunk(" Email : sihag.enterprise@rediffmail.com     ",
                FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0, 51, 153))));
        contact.add(new Chunk(" 9033 777 516 | 94260 36076",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(204, 0, 0))));
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(5);
        document.add(contact);
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
            PdfContentByte cb = writer.getDirectContent();

            try {
                if (headerImage != null) {
                    headerImage.setAbsolutePosition(margin, pageHeight - margin - headerHeight + 8);
                    cb.addImage(headerImage);

                    float lineY = pageHeight - margin - headerHeight + 5;
                    cb.setColorStroke(new Color(0, 51, 102));
                    cb.setLineWidth(1);
                    cb.moveTo(margin, lineY);
                    cb.lineTo(pageWidth - margin, lineY);
                    cb.stroke();
                }

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
