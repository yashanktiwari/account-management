package com.accounting.util;

import com.accounting.model.LorryReceipt;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class LorryReceiptPDFGenerator {

    private static final Logger log = AppLogger.get(LorryReceiptPDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String IMAGES_FOLDER = "src/main/resources/images";

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

    public static void generateLorryReceiptPDF(LorryReceipt lr, String outputPath, String copyLabel) {
        try {
            float margin = 14;
            Document document = new Document(PageSize.A4, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // ═══════════════════════════════════════════════════
            // OUTER BORDER TABLE (entire form wrapped in one cell)
            // ═══════════════════════════════════════════════════
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);

            PdfPCell outerCell = new PdfPCell();
            outerCell.setBorder(Rectangle.BOX);
            outerCell.setBorderWidth(1.5f);
            outerCell.setPadding(0);

            // ╔════════════════════════════════════════════════╗
            // ║  TOP SECTION: 3-column grid                   ║
            // ║  Col1: LR_NOTICE (1/3)  Col2: RISK (1/3)     ║
            // ║  Col3: CONSIGNEE COPY (1/3, full height)      ║
            // ║  LR_HEADER spans Col1+Col2 at top             ║
            // ╚════════════════════════════════════════════════╝
            buildTopSection(outerCell, lr);

            // ╔════════════════════════════════════════════════╗
            // ║  SECTION 3 — BOTTOM (full width, generated)    ║
            // ╚════════════════════════════════════════════════╝
            buildBottomSection(outerCell, lr);

            outerTable.addCell(outerCell);
            document.add(outerTable);
            document.close();
            log.info("Lorry Receipt PDF generated: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to generate Lorry Receipt PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TOP SECTION: 2-row, 3-column grid
    //  Row 1: Col1+Col2 merged (LR_HEADER), Col3 (CONSIGNEE COPY top)
    //  Row 2: Col1 (LR_NOTICE), Col2 (AT OWNER'S RISK), Col3 (CONSIGNEE COPY continuation)
    // ══════════════════════════════════════════════════════════
    private static void buildTopSection(PdfPCell outerCell, LorryReceipt lr) throws Exception {
        PdfPTable topGrid = new PdfPTable(3);
        topGrid.setWidthPercentage(100);
        topGrid.setWidths(new float[]{1f, 1f, 1f});

        // ── ROW 1 ──
        // ── Col1+Col2 merged: LR_HEADER (2/3 width) ──
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.BOX);
        headerCell.setPadding(2);
        headerCell.setColspan(2);
        headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image headerImg = loadImage("LR_HEADER");
        if (headerImg != null) {
            float maxW = (PageSize.A4.getWidth() - 28) * 2f / 3f - 6;
            headerImg.scaleToFit(maxW, 200);
            headerImg.setAlignment(Image.MIDDLE);
            headerCell.addElement(headerImg);
        } else {
            headerCell.addElement(new Paragraph("[ LR_HEADER.jpg — place image in src/main/resources/images/ ]",
                    F_NORM_8));
        }
        topGrid.addCell(headerCell);

        // ── Col3: CONSIGNEE COPY (1/3 width, spans to row 2) ──
        PdfPCell ccCell = new PdfPCell();
        ccCell.setBorder(Rectangle.BOX);
        ccCell.setPadding(0);
        ccCell.setRowspan(2); // Spans both rows

        PdfPTable ccInner = new PdfPTable(1);
        ccInner.setWidthPercentage(100);

        Paragraph ccTitle = new Paragraph("CONSIGNEE COPY", F_BOLD_12);
        ccTitle.setAlignment(Element.ALIGN_CENTER);
        PdfPCell ccTitleCell = new PdfPCell(ccTitle);
        ccTitleCell.setBorder(Rectangle.BOTTOM);
        ccTitleCell.setBorderWidthBottom(1f);
        ccTitleCell.setPadding(8);
        ccTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        ccInner.addCell(ccTitleCell);

        // LR field rows
        PdfPTable lrFields = new PdfPTable(2);
        lrFields.setWidthPercentage(100);
        lrFields.setWidths(new float[]{1.2f, 1.8f});

        addLabelValueRow(lrFields, "LR No.", s(lr.getLrNo()), F_RED_BOLD_14);
        addLabelValueRow(lrFields, "LR Date", lr.getLrDate() != null ? lr.getLrDate().format(DATE_FORMATTER) : "", F_NORM_10);
        addLabelValueRow(lrFields, "Vehicle No.", s(lr.getVehicleNo()), F_BOLD_10);
        addLabelValueRow(lrFields, "From", s(lr.getFromLocation()), F_BOLD_10);
        addLabelValueRow(lrFields, "To", s(lr.getToLocation()), F_BOLD_10);
        addLabelValueRow(lrFields, "E-Way Bill No.", s(lr.getEWayBillNo()), F_NORM_10);

        PdfPCell lrFieldsWrap = new PdfPCell(lrFields);
        lrFieldsWrap.setBorder(Rectangle.NO_BORDER);
        lrFieldsWrap.setPadding(0);
        ccInner.addCell(lrFieldsWrap);

        // Add spacer to push content and fill space
        PdfPCell spacerCell = new PdfPCell(new Paragraph(" ", F_NORM_8));
        spacerCell.setBorder(Rectangle.NO_BORDER);
        spacerCell.setPadding(80);
        ccInner.addCell(spacerCell);

        ccCell.addElement(ccInner);
        topGrid.addCell(ccCell);

        // ── ROW 2 ──
        // ── Col1: LR_NOTICE (1/3 width) ──
        PdfPCell noticeCell = new PdfPCell();
        noticeCell.setBorder(Rectangle.BOX);
        noticeCell.setPadding(2);
        noticeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image noticeImg = loadImage("LR_NOTICE");
        if (noticeImg != null) {
            float maxW = (PageSize.A4.getWidth() - 28) / 3f - 6;
            noticeImg.scaleToFit(maxW, 150);
            noticeImg.setAlignment(Image.MIDDLE);
            noticeCell.addElement(noticeImg);
        } else {
            noticeCell.addElement(new Paragraph("[ LR_NOTICE.jpg — place image in src/main/resources/images/ ]",
                    F_NORM_8));
        }
        topGrid.addCell(noticeCell);

        // ── Col2: AT OWNER'S RISK (1/3 width) ──
        PdfPCell riskCell = new PdfPCell();
        riskCell.setBorder(Rectangle.BOX);
        riskCell.setPadding(2);

        Paragraph riskTitle = new Paragraph("AT OWNER'S RISK / CARRIER'S RISK", F_BOLD_9);
        riskTitle.setAlignment(Element.ALIGN_CENTER);
        riskTitle.setLeading(10, 0);
        riskCell.addElement(riskTitle);

        Paragraph line1 = new Paragraph("I the Costumer has stated that He has not", F_NORM_7);
        line1.setLeading(9, 0);
        riskCell.addElement(line1);

        Paragraph line2 = new Paragraph("insured the consignment or", F_NORM_7);
        line2.setLeading(9, 0);
        riskCell.addElement(line2);

        Paragraph line3 = new Paragraph("He has insured the consignment", F_NORM_7);
        line3.setLeading(9, 0);
        riskCell.addElement(line3);

        Paragraph companyLine = new Paragraph();
        companyLine.add(new Chunk("Company ", F_NORM_7));
        companyLine.add(new Chunk(s(lr.getInsuranceCompany()).isEmpty() ? "______________________________________" : s(lr.getInsuranceCompany()), F_NORM_8));
        companyLine.setLeading(9, 0);
        riskCell.addElement(companyLine);

        Paragraph policyLine = new Paragraph();
        policyLine.add(new Chunk("Policy No. ", F_NORM_7));
        policyLine.add(new Chunk(s(lr.getPolicyNo()).isEmpty() ? "__________________" : s(lr.getPolicyNo()), F_NORM_8));
        policyLine.add(new Chunk("    Date ", F_NORM_7));
        policyLine.add(new Chunk(lr.getPolicyDate() != null ? lr.getPolicyDate().format(DATE_FORMATTER) : "____________", F_NORM_8));
        policyLine.setLeading(9, 0);
        riskCell.addElement(policyLine);

        Paragraph amountLine = new Paragraph();
        amountLine.add(new Chunk("Amount ", F_NORM_7));
        amountLine.add(new Chunk(s(lr.getInsuranceAmount()).isEmpty() ? "___________________" : s(lr.getInsuranceAmount()), F_NORM_8));
        amountLine.add(new Chunk("    Date ", F_NORM_7));
        amountLine.add(new Chunk(lr.getInsuranceDate() != null ? lr.getInsuranceDate().format(DATE_FORMATTER) : "____________", F_NORM_8));
        amountLine.setLeading(9, 0);
        riskCell.addElement(amountLine);

        topGrid.addCell(riskCell);

        outerCell.addElement(topGrid);
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 3 — BOTTOM (full width, all generated as per image)
    //  Consignor/Consignee → Table → S.T./Weights → Disclaimer → Footer
    // ══════════════════════════════════════════════════════════
    private static void buildBottomSection(PdfPCell outerCell, LorryReceipt lr) throws Exception {

        // ── 3a. Consignor / Consignee ──
        PdfPTable conTable = new PdfPTable(2);
        conTable.setWidthPercentage(100);
        conTable.setWidths(new float[]{1f, 1f});

        PdfPCell csorCell = new PdfPCell();
        csorCell.setBorder(Rectangle.BOX);
        csorCell.setPadding(4);
        csorCell.setMinimumHeight(22);
        Paragraph csorPara = new Paragraph();
        csorPara.add(new Chunk("Consignor", F_BOLD_9));
        csorPara.add(new Chunk("  " + s(lr.getConsignorName()), F_NORM_9));
        csorCell.addElement(csorPara);
        conTable.addCell(csorCell);

        PdfPCell cseeCell = new PdfPCell();
        cseeCell.setBorder(Rectangle.BOX);
        cseeCell.setPadding(4);
        cseeCell.setMinimumHeight(22);
        Paragraph cseePara = new Paragraph();
        cseePara.add(new Chunk("Consignee", F_BOLD_9));
        cseePara.add(new Chunk("  " + s(lr.getConsigneeName()), F_NORM_9));
        cseeCell.addElement(cseePara);
        conTable.addCell(cseeCell);

        // GSTIN row
        PdfPCell gstinCsor = new PdfPCell();
        gstinCsor.setBorder(Rectangle.BOX);
        gstinCsor.setPadding(4);
        Paragraph gstinCsorPara = new Paragraph();
        gstinCsorPara.add(new Chunk("GSTIN : ", F_BOLD_9));
        gstinCsorPara.add(new Chunk(s(lr.getConsignorGstin()), F_NORM_9));
        gstinCsor.addElement(gstinCsorPara);
        conTable.addCell(gstinCsor);

        PdfPCell gstinCsee = new PdfPCell();
        gstinCsee.setBorder(Rectangle.BOX);
        gstinCsee.setPadding(4);
        Paragraph gstinCseePara = new Paragraph();
        gstinCseePara.add(new Chunk("GSTIN : ", F_BOLD_9));
        gstinCseePara.add(new Chunk(s(lr.getConsigneeGstin()), F_NORM_9));
        gstinCsee.addElement(gstinCseePara);
        conTable.addCell(gstinCsee);

        outerCell.addElement(conTable);

        // ── 3b. Main table: left (packages/desc/weights) + right (freight amounts) ──
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{3.2f, 1.8f});

        // ─── LEFT SIDE ───
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setPadding(0);

        // Package header table (8 columns, 2-row header)
        PdfPTable pkgTable = new PdfPTable(8);
        pkgTable.setWidthPercentage(100);
        pkgTable.setWidths(new float[]{0.7f, 0.8f, 2.2f, 0.7f, 0.7f, 0.5f, 0.7f, 0.7f});

        // Row 1 headers (rowspan=2 for 4 cols, colspan=2 for WEIGHT and FREIGHT)
        addHeaderCellRowspan(pkgTable, "No. of\nPackages", 2);
        addHeaderCellRowspan(pkgTable, "Method of\nPacking", 2);
        addHeaderCellRowspan(pkgTable, "DESCRIPTION (Said to Contain)", 2);

        PdfPCell wh = new PdfPCell(new Phrase("WEIGHT", F_BOLD_8));
        wh.setColspan(2);
        wh.setBorder(Rectangle.BOX);
        wh.setPadding(3);
        wh.setHorizontalAlignment(Element.ALIGN_CENTER);
        wh.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pkgTable.addCell(wh);

        addHeaderCellRowspan(pkgTable, "Rate", 2);

        PdfPCell fh = new PdfPCell(new Phrase("FREIGHT", F_BOLD_8));
        fh.setColspan(2);
        fh.setBorder(Rectangle.BOX);
        fh.setPadding(3);
        fh.setHorizontalAlignment(Element.ALIGN_CENTER);
        fh.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pkgTable.addCell(fh);

        // Row 2 sub-headers (only 4 cells needed — under WEIGHT and FREIGHT)
        addSubHeader(pkgTable, "Actual");
        addSubHeader(pkgTable, "Charged");
        addSubHeaderTwoLine(pkgTable, "TO PAY", "Rs.       Ps.");
        addSubHeaderTwoLine(pkgTable, "PAID", "Rs.       Ps.");

        // Data row with actual values
        addDataCell(pkgTable, s(lr.getNoOfPackages()), 30);
        addDataCell(pkgTable, s(lr.getMethodOfPacking()), 30);
        addDataCell(pkgTable, s(lr.getDescription()), 30);
        addDataCell(pkgTable, s(lr.getWeightActual()), 30);
        addDataCell(pkgTable, s(lr.getWeightCharged()), 30);
        addDataCell(pkgTable, s(lr.getRate()), 30);
        addDataCell(pkgTable, lr.getFreightToPay() > 0 ? String.format("%.2f", lr.getFreightToPay()) : "", 30);
        addDataCell(pkgTable, lr.getFreightPaid() > 0 ? String.format("%.2f", lr.getFreightPaid()) : "", 30);

        // Empty spacer rows (2 rows to give height, like the physical form)
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 8; c++) {
                PdfPCell e = new PdfPCell(new Phrase(" ", F_NORM_8));
                e.setBorder(Rectangle.BOX);
                e.setMinimumHeight(16);
                e.setPadding(2);
                pkgTable.addCell(e);
            }
        }

        leftCell.addElement(pkgTable);

        // S.T. No / S.H. No / Value / G.Wt / T.Wt / N.Wt sub-table
        PdfPTable stTable = new PdfPTable(6);
        stTable.setWidthPercentage(100);
        stTable.setWidths(new float[]{1f, 0.7f, 1f, 0.7f, 0.7f, 0.9f});

        // Row 1: S.T. No [box] S.H. No [box]   G. Wt. [value]
        addBorderedCell(stTable, "S.T. No.", F_BOLD_8, Element.ALIGN_LEFT);
        addBorderedCell(stTable, s(lr.getStNo()), F_NORM_9, Element.ALIGN_CENTER);
        addBorderedCell(stTable, "S.H. No.", F_BOLD_8, Element.ALIGN_LEFT);
        addBorderedCell(stTable, s(lr.getShNo()), F_NORM_9, Element.ALIGN_CENTER);
        addBorderedCell(stTable, "G. Wt.", F_BOLD_8, Element.ALIGN_LEFT);
        addBorderedCell(stTable, s(lr.getGrossWeight()), F_NORM_9, Element.ALIGN_LEFT);

        // Row 2: Value Rs. [box colspan=3]   T. Wt. [value]
        addBorderedCell(stTable, "Value Rs.", F_BOLD_8, Element.ALIGN_LEFT);
        PdfPCell valSpan = new PdfPCell(new Phrase(s(lr.getValueRs()), F_NORM_9));
        valSpan.setColspan(3);
        valSpan.setBorder(Rectangle.BOX);
        valSpan.setPadding(3);
        valSpan.setMinimumHeight(16);
        stTable.addCell(valSpan);
        addBorderedCell(stTable, "T. Wt.", F_BOLD_8, Element.ALIGN_LEFT);
        addBorderedCell(stTable, s(lr.getTareWeight()), F_NORM_9, Element.ALIGN_LEFT);

        // Row 3: [empty colspan=4]   N. Wt. [value]
        PdfPCell emptySpan = new PdfPCell(new Phrase(" ", F_NORM_8));
        emptySpan.setColspan(4);
        emptySpan.setBorder(Rectangle.BOX);
        emptySpan.setPadding(3);
        emptySpan.setMinimumHeight(16);
        stTable.addCell(emptySpan);
        addBorderedCell(stTable, "N. Wt.", F_BOLD_8, Element.ALIGN_LEFT);
        addBorderedCell(stTable, s(lr.getNetWeight()), F_NORM_9, Element.ALIGN_LEFT);

        leftCell.addElement(stTable);
        mainTable.addCell(leftCell);

        // ─── RIGHT SIDE: Freight amounts ───
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setPadding(0);

        PdfPTable amtTable = new PdfPTable(2);
        amtTable.setWidthPercentage(100);
        amtTable.setWidths(new float[]{1.2f, 1f});

        // Empty spacer rows to match left table header height
        for (int i = 0; i < 4; i++) {
            PdfPCell ec = new PdfPCell(new Phrase(" ", F_NORM_8));
            ec.setBorder(Rectangle.BOX);
            ec.setMinimumHeight(16);
            ec.setPadding(2);
            ec.setColspan(2);
            amtTable.addCell(ec);
        }

        // Amount rows
        addAmountRow(amtTable, "Freight", lr.getFreight());
        addAmountRow(amtTable, "Advance", lr.getAdvance());
        addAmountRow(amtTable, "Balance", lr.getBalance());
        addAmountRow(amtTable, "A.O.C.", lr.getAoc());
        addAmountRow(amtTable, "S.T. Charge", lr.getStCharge());
        addAmountRow(amtTable, "Total", lr.getTotal());

        rightCell.addElement(amtTable);
        mainTable.addCell(rightCell);

        outerCell.addElement(mainTable);

        // ── 3c. Disclaimer + "For, SIHAG ENTERPRISE" ──
        PdfPTable discTable = new PdfPTable(2);
        discTable.setWidthPercentage(100);
        discTable.setWidths(new float[]{3.2f, 1.8f});

        PdfPCell discCell = new PdfPCell();
        discCell.setBorder(Rectangle.BOX);
        discCell.setPadding(4);
        discCell.addElement(new Paragraph(
                "The Consignment note issued subject to terms & condition printed overleaf", F_NORM_7));
        discCell.addElement(new Paragraph(
                "We are not responsible for the leakage & Breakage.", F_NORM_7));
        discTable.addCell(discCell);

        PdfPCell forCell = new PdfPCell();
        forCell.setBorder(Rectangle.BOX);
        forCell.setPadding(4);
        forCell.setMinimumHeight(40);
        Paragraph forPara = new Paragraph("For, SIHAG ENTERPRISE", F_BOLD_11);
        forPara.setAlignment(Element.ALIGN_CENTER);
        forCell.addElement(forPara);
        discTable.addCell(forCell);

        outerCell.addElement(discTable);

        // ── 3d. Footer: To Pay Rs / Adv. Paid Rs / Inv. No / Inv. Date ──
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        PdfPCell footCell = new PdfPCell();
        footCell.setBorder(Rectangle.BOX);
        footCell.setPadding(5);

        Paragraph toPayLine = new Paragraph();
        toPayLine.add(new Chunk("To Pay Rs. ", F_BOLD_9));
        toPayLine.add(new Chunk(lr.getToPayRs() > 0 ? String.format("%.2f", lr.getToPayRs()) : "________________________", F_NORM_9));
        toPayLine.add(new Chunk("     Adv. Paid Rs. ", F_BOLD_9));
        toPayLine.add(new Chunk(lr.getAdvPaidRs() > 0 ? String.format("%.2f", lr.getAdvPaidRs()) : "________________________", F_NORM_9));
        footCell.addElement(toPayLine);

        footCell.addElement(spacer(4));

        Paragraph invLine = new Paragraph();
        invLine.add(new Chunk("Inv. No. ", F_BOLD_9));
        invLine.add(new Chunk(s(lr.getInvNo()).isEmpty() ? "________________________" : s(lr.getInvNo()), F_NORM_9));
        invLine.add(new Chunk("          Inv. Date ", F_BOLD_9));
        invLine.add(new Chunk(lr.getInvDate() != null ? lr.getInvDate().format(DATE_FORMATTER) : "________________________", F_NORM_9));
        footCell.addElement(invLine);

        footerTable.addCell(footCell);
        outerCell.addElement(footerTable);
    }

    // ══════════════════════════════════════════════
    //  Helper methods
    // ══════════════════════════════════════════════

    private static String s(String val) {
        return val != null ? val : "";
    }

    private static Paragraph spacer(float size) {
        return new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, size));
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

    private static void addLabelValueRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, F_BOLD_9));
        lc.setBorder(Rectangle.BOX);
        lc.setPadding(3);
        lc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.BOX);
        vc.setPadding(3);
        vc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(vc);
    }

    private static void addHeaderCellRowspan(PdfPTable table, String text, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_BOLD_8));
        cell.setRowspan(rowspan);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addSubHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_BOLD_8));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addSubHeaderTwoLine(PdfPTable table, String line1, String line2) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph p1 = new Paragraph(line1, F_BOLD_7);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(line2, F_BOLD_7);
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }

    private static void addDataCell(PdfPTable table, String text, float minHeight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_NORM_9));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setMinimumHeight(minHeight);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addBorderedCell(PdfPTable table, String text, Font font, int hAlign) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(3);
        cell.setMinimumHeight(16);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(hAlign);
        table.addCell(cell);
    }

    private static void addAmountRow(PdfPTable table, String label, double value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, F_BOLD_9));
        lc.setBorder(Rectangle.BOX);
        lc.setPadding(3);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value > 0 ? String.format("%.2f", value) : "", F_NORM_9));
        vc.setBorder(Rectangle.BOX);
        vc.setPadding(3);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(vc);
    }
}
