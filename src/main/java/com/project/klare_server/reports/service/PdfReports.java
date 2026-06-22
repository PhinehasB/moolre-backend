package com.project.klare_server.reports.service;

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
import com.project.klare_server.payroll.domain.PayrollItem;
import com.project.klare_server.payroll.domain.PayrollRun;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfReports {

    private static final Color GREEN = new Color(0x15, 0x60, 0x4A);
    private static final Color HEADER_BG = new Color(0xEE, 0xF1, 0xEE);
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);

    public byte[] payrollRunPdf(String companyName, String period, PayrollRun run, List<PayrollItem> items) {
        Document document = new Document(PageSize.A4, 48, 48, 54, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        title(document, "Payroll Run Report", companyName, period);

        PdfPTable table = new PdfPTable(new float[]{5, 3, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(18f);
        headerCell(table, "Employee");
        headerCell(table, "Amount (GHS)");
        headerCell(table, "Status");
        for (PayrollItem item : items) {
            bodyCell(table, item.getEmployeeName(), Element.ALIGN_LEFT);
            bodyCell(table, item.getAmount().toPlainString(), Element.ALIGN_RIGHT);
            bodyCell(table, item.getStatus().name(), Element.ALIGN_LEFT);
        }
        document.add(table);

        int successRate = run.getEmployeeCount() == 0 ? 0
                : Math.round(run.getSuccessCount() * 100f / run.getEmployeeCount());
        Paragraph summary = new Paragraph();
        summary.setSpacingBefore(16f);
        summary.add(metric("Employees", String.valueOf(run.getEmployeeCount())));
        summary.add(metric("Total payroll", "GHS " + run.getTotalAmount().toPlainString()));
        summary.add(metric("Service fee", "GHS " + safe(run.getServiceFee()).toPlainString()));
        summary.add(metric("Success rate", successRate + "%"));
        document.add(summary);

        footer(document);
        document.close();
        return out.toByteArray();
    }

    public byte[] taxSummaryPdf(String companyName, int year, List<PayrollRun> runs) {
        Document document = new Document(PageSize.A4, 48, 48, 54, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        title(document, "Annual Tax Summary", companyName, year + " (year to date)");

        PdfPTable table = new PdfPTable(new float[]{3, 2, 3, 3});
        table.setWidthPercentage(100);
        table.setSpacingBefore(18f);
        headerCell(table, "Period");
        headerCell(table, "Employees");
        headerCell(table, "Total paid (GHS)");
        headerCell(table, "Service fee (GHS)");

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        for (PayrollRun run : runs) {
            bodyCell(table, monthLabel(run.getPeriodMonth()) + " " + run.getPeriodYear(), Element.ALIGN_LEFT);
            bodyCell(table, String.valueOf(run.getEmployeeCount()), Element.ALIGN_RIGHT);
            bodyCell(table, run.getTotalAmount().toPlainString(), Element.ALIGN_RIGHT);
            bodyCell(table, safe(run.getServiceFee()).toPlainString(), Element.ALIGN_RIGHT);
            totalPaid = totalPaid.add(run.getTotalAmount());
            totalFees = totalFees.add(safe(run.getServiceFee()));
        }
        totalCell(table, "Total");
        totalCell(table, "");
        totalCell(table, totalPaid.toPlainString());
        totalCell(table, totalFees.toPlainString());
        document.add(table);

        footer(document);
        document.close();
        return out.toByteArray();
    }

    private void title(Document document, String reportTitle, String companyName, String period) {
        Paragraph brand = new Paragraph("Klare", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, GREEN));
        document.add(brand);
        Paragraph heading = new Paragraph(reportTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK));
        heading.setSpacingBefore(6f);
        document.add(heading);
        Paragraph meta = new Paragraph(companyName + "  ·  " + period,
                FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED));
        meta.setSpacingBefore(2f);
        document.add(meta);
    }

    private void footer(Document document) {
        Paragraph footer = new Paragraph(
                "Generated by Klare · Powered by Moolre · " + LocalDate.now(ZoneOffset.UTC),
                FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED));
        footer.setSpacingBefore(24f);
        document.add(footer);
    }

    private void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED)));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorder(0);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void bodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK)));
        cell.setBorderColor(new Color(0xEC, 0xEC, 0xEC));
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setBorderWidthTop(0);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void totalCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK)));
        cell.setBorder(0);
        cell.setPaddingTop(10f);
        cell.setHorizontalAlignment("Total".equals(text) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private Phrase metric(String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Phrase phrase = new Phrase();
        phrase.add(new com.lowagie.text.Chunk(label + ": ", labelFont));
        phrase.add(new com.lowagie.text.Chunk(value + "\n", valueFont));
        return phrase;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String monthLabel(int month) {
        return java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
    }
}
