package com.accounting.util;

import com.accounting.model.LorryReceipt;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class LorryReceiptPDFGenerator {

    private static final Logger log = AppLogger.get(LorryReceiptPDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Fonts
    private static final Font F_BOLD_14 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font F_BOLD_12 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font F_BOLD_11 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font F_BOLD_10 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font F_BOLD_9 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font F_BOLD_8 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font F_BOLD_7 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
    private static final Font F_NORM_10 = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font F_NORM_9 = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font F_NORM_8 = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font F_NORM_7 = FontFactory.getFont(FontFactory.HELVETICA, 7);
    private static final Font F_RED_BOLD_14 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(204, 0, 0));

    private static final Color DARK_BLUE = new Color(0, 51, 102);

    public static void generateLorryReceiptPDF(LorryReceipt lr, String outputPath, String copyLabel) {
        try {
            float margin = 14;
            Document document = new Document(PageSize.A4, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            float fullWidth = PageSize.A4.getWidth() - 2 * margin;

            // ═══════════════════════════════════════════
            // OUTER BORDER TABLE (entire form in one cell)
            // ═══════════════════════════════════════════
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);

            PdfPCell outerCell = new PdfPCell();
            outerCell.setBorder(Rectangle.BOX);
            outerCell.setBorderWidth(1.5f);
            outerCell.setPadding(0);

            // ─── ROW 1: Header area (GST/PAN line + Company name + right-side LR fields) ───
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3.2f, 1.8f});

            // LEFT: Company header
            PdfPCell leftHeaderCell = new PdfPCell();
            leftHeaderCell.setBorder(Rectangle.BOX);
            leftHeaderCell.setPadding(4);

            // GST / PAN / Subject / Name / Mobile line
            Paragraph gstLine = new Paragraph();
            gstLine.add(new Chunk("GST : 24DYHPD9230H1ZJ", F_BOLD_8));
            gstLine.add(new Chunk("          Subject to Mundra Jurisdiction", F_NORM_7));
            gstLine.add(new Chunk("              Suresh Sihag", F_BOLD_9));
            leftHeaderCell.addElement(gstLine);

            Paragraph panLine = new Paragraph();
            panLine.add(new Chunk("PAN : DYHPD9230H", F_BOLD_8));
            panLine.add(new Chunk("                                                                             M. 90337 77516 / 94260 36076", F_NORM_7));
            leftHeaderCell.addElement(panLine);

            // Company name
            leftHeaderCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 2)));
            Paragraph companyName = new Paragraph();
            companyName.add(new Chunk("SIHAG ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, DARK_BLUE)));
            companyName.add(new Chunk("ENTERPRISE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(204, 0, 0))));
            companyName.setAlignment(Element.ALIGN_CENTER);
            leftHeaderCell.addElement(companyName);

            // Tagline
            Paragraph tagline = new Paragraph("SPECIALIST CONTAINER HANDLERS, FLEET OWNERS, TRANSPORT CONTRACTORS & COMMISSION AGENT", F_BOLD_7);
            tagline.setAlignment(Element.ALIGN_CENTER);
            leftHeaderCell.addElement(tagline);

            // Address
            Paragraph address = new Paragraph("Office No. 03, Commercial Zone, Survey No. 33/1, Plot No.1, Nr. Shahid Manshi Circle, Adani Port Road,", F_NORM_7);
            address.setAlignment(Element.ALIGN_CENTER);
            leftHeaderCell.addElement(address);

            Paragraph address2 = new Paragraph("Nanakapaya, Mundra, Kutch. 370421  |  Email : sihag.enterprise23@rediffmail.com", F_NORM_7);
            address2.setAlignment(Element.ALIGN_CENTER);
            leftHeaderCell.addElement(address2);

            headerTable.addCell(leftHeaderCell);

            // RIGHT: CONSIGNEE COPY + LR fields
            PdfPCell rightHeaderCell = new PdfPCell();
            rightHeaderCell.setBorder(Rectangle.BOX);
            rightHeaderCell.setPadding(0);

            // CONSIGNEE COPY label
            Paragraph consigneeCopy = new Paragraph("CONSIGNEE COPY", F_BOLD_12);
            consigneeCopy.setAlignment(Element.ALIGN_CENTER);
            PdfPCell ccCell = new PdfPCell(consigneeCopy);
            ccCell.setBorder(Rectangle.BOTTOM);
            ccCell.setPadding(4);
            ccCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            // LR fields table
            PdfPTable lrFieldsTable = new PdfPTable(2);
            lrFieldsTable.setWidthPercentage(100);
            lrFieldsTable.setWidths(new float[]{1.2f, 1.8f});

            addLabelValueRow(lrFieldsTable, "LR No.", lr.getLrNo() != null ? lr.getLrNo() : "", F_RED_BOLD_14);
            addLabelValueRow(lrFieldsTable, "LR Date", lr.getLrDate() != null ? lr.getLrDate().format(DATE_FORMATTER) : "", F_NORM_10);
            addLabelValueRow(lrFieldsTable, "Vehicle No.", lr.getVehicleNo() != null ? lr.getVehicleNo() : "", F_BOLD_10);
            addLabelValueRow(lrFieldsTable, "From", lr.getFromLocation() != null ? lr.getFromLocation() : "", F_BOLD_10);
            addLabelValueRow(lrFieldsTable, "To", lr.getToLocation() != null ? lr.getToLocation() : "", F_BOLD_10);
            addLabelValueRow(lrFieldsTable, "E-Way Bill No.", lr.getEWayBillNo() != null ? lr.getEWayBillNo() : "", F_NORM_10);

            // Nest cc + lr fields into right cell
            PdfPTable rightInner = new PdfPTable(1);
            rightInner.setWidthPercentage(100);
            rightInner.addCell(ccCell);
            PdfPCell lrFieldsCell = new PdfPCell(lrFieldsTable);
            lrFieldsCell.setBorder(Rectangle.NO_BORDER);
            lrFieldsCell.setPadding(0);
            rightInner.addCell(lrFieldsCell);

            rightHeaderCell.addElement(rightInner);
            headerTable.addCell(rightHeaderCell);

            outerCell.addElement(headerTable);

            // ─── ROW 2: Notice + AT OWNER'S RISK / CARRIER'S RISK ───
            PdfPTable noticeRiskTable = new PdfPTable(2);
            noticeRiskTable.setWidthPercentage(100);
            noticeRiskTable.setWidths(new float[]{1f, 1f});

            // Notice
            PdfPCell noticeCell = new PdfPCell();
            noticeCell.setBorder(Rectangle.BOX);
            noticeCell.setPadding(4);

            Paragraph noticeTitle = new Paragraph("Notice", F_BOLD_9);
            noticeTitle.setAlignment(Element.ALIGN_CENTER);
            noticeCell.addElement(noticeTitle);

            Paragraph noticeText = new Paragraph(
                    "The Consignment converted by this set of Special Lorry " +
                    "Receipt From shall be stored at the destination under the " +
                    "control of the Transport Operator and shall be delivered to ortho " +
                    "the order of the Consignee Bank whose name is mentioned in " +
                    "the Lorry Receipt It will under no circumstance be delivered to " +
                    "any one without the written authority from the Consignee Bank " +
                    "or its order endorsed on the Consignee copy or on a separate " +
                    "letter or Authority.",
                    F_NORM_7);
            noticeText.setAlignment(Element.ALIGN_JUSTIFIED);
            noticeCell.addElement(noticeText);
            noticeRiskTable.addCell(noticeCell);

            // AT OWNER'S RISK / CARRIER'S RISK
            PdfPCell riskCell = new PdfPCell();
            riskCell.setBorder(Rectangle.BOX);
            riskCell.setPadding(4);

            String riskType = lr.getRiskType() != null ? lr.getRiskType() : "OWNER'S RISK";
            Paragraph riskTitle = new Paragraph("AT " + riskType, F_BOLD_9);
            riskTitle.setAlignment(Element.ALIGN_CENTER);
            riskCell.addElement(riskTitle);

            riskCell.addElement(new Paragraph("I the Customer has stated that He has not", F_NORM_7));
            riskCell.addElement(new Paragraph("insured the consignment or", F_NORM_7));
            riskCell.addElement(new Paragraph("He has insured the consignment", F_NORM_7));
            riskCell.addElement(new Paragraph("Company " + s(lr.getInsuranceCompany()), F_NORM_7));
            riskCell.addElement(new Paragraph("Policy No. " + s(lr.getPolicyNo()) + "     Date " +
                    (lr.getPolicyDate() != null ? lr.getPolicyDate().format(DATE_FORMATTER) : "___________"), F_NORM_7));
            riskCell.addElement(new Paragraph("Amount " + s(lr.getInsuranceAmount()) + "     Date " +
                    (lr.getInsuranceDate() != null ? lr.getInsuranceDate().format(DATE_FORMATTER) : "___________"), F_NORM_7));

            noticeRiskTable.addCell(riskCell);
            outerCell.addElement(noticeRiskTable);

            // ─── ROW 3: Consignor / Consignee ───
            PdfPTable conTable = new PdfPTable(2);
            conTable.setWidthPercentage(100);
            conTable.setWidths(new float[]{1f, 1f});

            // Consignor
            PdfPCell consignorCell = new PdfPCell();
            consignorCell.setBorder(Rectangle.BOX);
            consignorCell.setPadding(4);
            consignorCell.addElement(new Paragraph("Consignor  " + s(lr.getConsignorName()), F_NORM_9));
            consignorCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            consignorCell.addElement(new Paragraph("GSTIN :  " + s(lr.getConsignorGstin()), F_NORM_9));
            conTable.addCell(consignorCell);

            // Consignee
            PdfPCell consigneeCell = new PdfPCell();
            consigneeCell.setBorder(Rectangle.BOX);
            consigneeCell.setPadding(4);
            consigneeCell.addElement(new Paragraph("Consignee  " + s(lr.getConsigneeName()), F_NORM_9));
            consigneeCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            consigneeCell.addElement(new Paragraph("GSTIN :  " + s(lr.getConsigneeGstin()), F_NORM_9));
            conTable.addCell(consigneeCell);

            outerCell.addElement(conTable);

            // ─── ROW 4: Main table (Packages, Description, Weight, Freight) + right amounts ───
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{3.2f, 1.8f});

            // LEFT: Package/Description/Weight table
            PdfPCell leftMainCell = new PdfPCell();
            leftMainCell.setBorder(Rectangle.BOX);
            leftMainCell.setPadding(0);

            // Sub-header row
            PdfPTable pkgHeaderTable = new PdfPTable(8);
            pkgHeaderTable.setWidthPercentage(100);
            pkgHeaderTable.setWidths(new float[]{0.7f, 0.8f, 2.0f, 0.8f, 0.8f, 0.6f, 0.8f, 0.8f});

            // Row 1 headers
            addHeaderCellRowspan(pkgHeaderTable, "No. of\nPackages", 2);
            addHeaderCellRowspan(pkgHeaderTable, "Method of\nPacking", 2);
            addHeaderCellRowspan(pkgHeaderTable, "DESCRIPTION (Said to Contain)", 2);

            // WEIGHT header spanning 2 cols
            PdfPCell weightHeader = new PdfPCell(new Phrase("WEIGHT", F_BOLD_8));
            weightHeader.setColspan(2);
            weightHeader.setBorder(Rectangle.BOX);
            weightHeader.setPadding(2);
            weightHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            weightHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            pkgHeaderTable.addCell(weightHeader);

            addHeaderCellRowspan(pkgHeaderTable, "Rate", 2);

            // FREIGHT header spanning 2 cols
            PdfPCell freightHeader = new PdfPCell(new Phrase("FREIGHT", F_BOLD_8));
            freightHeader.setColspan(2);
            freightHeader.setBorder(Rectangle.BOX);
            freightHeader.setPadding(2);
            freightHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            freightHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            pkgHeaderTable.addCell(freightHeader);

            // Row 2 sub-headers (under WEIGHT and FREIGHT)
            // Skip the 3 rowspan cells
            addSubHeader(pkgHeaderTable, "Actual");
            addSubHeader(pkgHeaderTable, "Charged");
            // Skip Rate rowspan
            addSubHeaderSmall(pkgHeaderTable, "TO PAY\nRs.     Ps.");
            addSubHeaderSmall(pkgHeaderTable, "PAID\nRs.     Ps.");

            // Data row
            addDataCell(pkgHeaderTable, s(lr.getNoOfPackages()));
            addDataCell(pkgHeaderTable, s(lr.getMethodOfPacking()));
            addDataCell(pkgHeaderTable, s(lr.getDescription()));
            addDataCell(pkgHeaderTable, s(lr.getWeightActual()));
            addDataCell(pkgHeaderTable, s(lr.getWeightCharged()));
            addDataCell(pkgHeaderTable, s(lr.getRate()));
            addDataCell(pkgHeaderTable, lr.getFreightToPay() > 0 ? String.format("%.2f", lr.getFreightToPay()) : "");
            addDataCell(pkgHeaderTable, lr.getFreightPaid() > 0 ? String.format("%.2f", lr.getFreightPaid()) : "");

            // Empty spacer rows for height
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 8; c++) {
                    PdfPCell empty = new PdfPCell(new Phrase(" ", F_NORM_8));
                    empty.setBorder(Rectangle.BOX);
                    empty.setMinimumHeight(14);
                    empty.setPadding(2);
                    pkgHeaderTable.addCell(empty);
                }
            }

            // Bottom section: S.T. No, S.H. No, Value, Weights
            PdfPTable bottomLeftTable = new PdfPTable(6);
            bottomLeftTable.setWidthPercentage(100);
            bottomLeftTable.setWidths(new float[]{1f, 0.6f, 1f, 0.6f, 0.8f, 1f});

            addBorderedCell(bottomLeftTable, "S.T. No.", F_BOLD_8);
            addBorderedCell(bottomLeftTable, s(lr.getStNo()), F_NORM_8);
            addBorderedCell(bottomLeftTable, "S.H. No.", F_BOLD_8);
            addBorderedCell(bottomLeftTable, s(lr.getShNo()), F_NORM_8);
            addBorderedCell(bottomLeftTable, "G. Wt.", F_BOLD_8);
            addBorderedCell(bottomLeftTable, s(lr.getGrossWeight()), F_NORM_8);

            addBorderedCell(bottomLeftTable, "Value Rs.", F_BOLD_8);
            PdfPCell valCell = new PdfPCell(new Phrase(s(lr.getValueRs()), F_NORM_8));
            valCell.setColspan(3);
            valCell.setBorder(Rectangle.BOX);
            valCell.setPadding(2);
            bottomLeftTable.addCell(valCell);
            addBorderedCell(bottomLeftTable, "T. Wt.", F_BOLD_8);
            addBorderedCell(bottomLeftTable, s(lr.getTareWeight()), F_NORM_8);

            // Empty row for N. Wt.
            PdfPCell emptySpan = new PdfPCell(new Phrase(" ", F_NORM_8));
            emptySpan.setColspan(4);
            emptySpan.setBorder(Rectangle.BOX);
            emptySpan.setPadding(2);
            bottomLeftTable.addCell(emptySpan);
            addBorderedCell(bottomLeftTable, "N. Wt.", F_BOLD_8);
            addBorderedCell(bottomLeftTable, s(lr.getNetWeight()), F_NORM_8);

            // Add sub-tables to left cell
            leftMainCell.addElement(pkgHeaderTable);
            leftMainCell.addElement(bottomLeftTable);
            mainTable.addCell(leftMainCell);

            // RIGHT: Freight amounts column
            PdfPCell rightMainCell = new PdfPCell();
            rightMainCell.setBorder(Rectangle.BOX);
            rightMainCell.setPadding(0);

            PdfPTable amountsTable = new PdfPTable(2);
            amountsTable.setWidthPercentage(100);
            amountsTable.setWidths(new float[]{1.2f, 1f});

            // Empty rows to align with the table on the left
            for (int i = 0; i < 4; i++) {
                PdfPCell ec1 = new PdfPCell(new Phrase(" ", F_NORM_8));
                ec1.setBorder(Rectangle.BOX);
                ec1.setMinimumHeight(14);
                ec1.setPadding(2);
                ec1.setColspan(2);
                amountsTable.addCell(ec1);
            }

            addAmountRow(amountsTable, "Freight", lr.getFreight());
            addAmountRow(amountsTable, "Advance", lr.getAdvance());
            addAmountRow(amountsTable, "Balance", lr.getBalance());
            addAmountRow(amountsTable, "A.O.C.", lr.getAoc());
            addAmountRow(amountsTable, "S.T. Charge", lr.getStCharge());
            addAmountRow(amountsTable, "Total", lr.getTotal());

            rightMainCell.addElement(amountsTable);
            mainTable.addCell(rightMainCell);

            outerCell.addElement(mainTable);

            // ─── ROW 5: Disclaimer + For SIHAG ENTERPRISE ───
            PdfPTable disclaimerTable = new PdfPTable(2);
            disclaimerTable.setWidthPercentage(100);
            disclaimerTable.setWidths(new float[]{3.2f, 1.8f});

            PdfPCell disclaimerCell = new PdfPCell();
            disclaimerCell.setBorder(Rectangle.BOX);
            disclaimerCell.setPadding(4);
            disclaimerCell.addElement(new Paragraph(
                    "The Consignment note issued subject to terms & condition printed overleaf", F_NORM_7));
            disclaimerCell.addElement(new Paragraph(
                    "We are not responsible for the leakage & Breakage.", F_NORM_7));
            disclaimerTable.addCell(disclaimerCell);

            PdfPCell forCell = new PdfPCell();
            forCell.setBorder(Rectangle.BOX);
            forCell.setPadding(4);
            Paragraph forPara = new Paragraph("For, SIHAG ENTERPRISE", F_BOLD_11);
            forPara.setAlignment(Element.ALIGN_CENTER);
            forCell.addElement(forPara);
            forCell.setMinimumHeight(40);
            disclaimerTable.addCell(forCell);

            outerCell.addElement(disclaimerTable);

            // ─── ROW 6: To Pay / Adv Paid / Inv No / Inv Date ───
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.BOX);
            footerCell.setPadding(4);

            Paragraph toPayLine = new Paragraph();
            toPayLine.add(new Chunk("To Pay Rs. ", F_BOLD_9));
            toPayLine.add(new Chunk(lr.getToPayRs() > 0 ? String.format("%.2f", lr.getToPayRs()) : "_______________", F_NORM_9));
            toPayLine.add(new Chunk("          Adv. Paid Rs. ", F_BOLD_9));
            toPayLine.add(new Chunk(lr.getAdvPaidRs() > 0 ? String.format("%.2f", lr.getAdvPaidRs()) : "_______________", F_NORM_9));
            footerCell.addElement(toPayLine);

            footerCell.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));

            Paragraph invLine = new Paragraph();
            invLine.add(new Chunk("Inv. No. ", F_BOLD_9));
            invLine.add(new Chunk(s(lr.getInvNo()).isEmpty() ? "_______________" : s(lr.getInvNo()), F_NORM_9));
            invLine.add(new Chunk("                    Inv. Date ", F_BOLD_9));
            invLine.add(new Chunk(lr.getInvDate() != null ? lr.getInvDate().format(DATE_FORMATTER) : "_______________", F_NORM_9));
            footerCell.addElement(invLine);

            footerTable.addCell(footerCell);
            outerCell.addElement(footerTable);

            outerTable.addCell(outerCell);
            document.add(outerTable);

            document.close();
            log.info("Lorry Receipt PDF generated successfully: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Lorry Receipt PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    // ── Helper methods ──

    private static String s(String val) {
        return val != null ? val : "";
    }

    private static void addLabelValueRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, F_BOLD_9));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setPadding(3);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setPadding(3);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valueCell);
    }

    private static void addHeaderCellRowspan(PdfPTable table, String text, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_BOLD_8));
        cell.setRowspan(rowspan);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addSubHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_BOLD_8));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addSubHeaderSmall(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_BOLD_7));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_NORM_8));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setMinimumHeight(16);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addBorderedCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setMinimumHeight(14);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addAmountRow(PdfPTable table, String label, double value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, F_BOLD_9));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setPadding(3);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value > 0 ? String.format("%.2f", value) : "", F_NORM_9));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(valueCell);
    }
}
