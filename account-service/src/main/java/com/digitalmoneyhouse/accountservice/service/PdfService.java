package com.digitalmoneyhouse.accountservice.service;

import com.digitalmoneyhouse.accountservice.dto.TransactionResponseDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Service
public class PdfService {

    public ByteArrayInputStream generateTransactionReceiptPdf(TransactionResponseDTO transaction) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(27, 54, 93));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph title = new Paragraph("Digital Money House", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Comprobante de Transacción", FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{30f, 70f});

            PdfPCell headerCell = new PdfPCell(new Phrase("Detalle", headerFont));
            headerCell.setBackgroundColor(new Color(27, 54, 93));
            headerCell.setColspan(2);
            headerCell.setPadding(8);
            table.addCell(headerCell);
            
            addRow(table, "ID Transacción:", String.valueOf(transaction.getId()), labelFont, valueFont);
            addRow(table, "Monto:", "$" + String.format("%.2f", transaction.getAmount()), labelFont, valueFont);
            addRow(table, "Tipo:", transaction.getType().name(), labelFont, valueFont);
            addRow(table, "Descripción:", transaction.getDescription() != null ? transaction.getDescription() : "N/A", labelFont, valueFont);
            addRow(table, "Fecha:", transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : "N/A", labelFont, valueFont);
            addRow(table, "CVU Origen:", transaction.getOriginCvu() != null ? transaction.getOriginCvu() : "N/A", labelFont, valueFont);
            addRow(table, "CVU Destino:", transaction.getDestCvu() != null ? transaction.getDestCvu() : "N/A", labelFont, valueFont);

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el archivo PDF del comprobante", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
        cLabel.setPadding(6);
        table.addCell(cLabel);

        PdfPCell cValue = new PdfPCell(new Phrase(value, valueFont));
        cValue.setPadding(6);
        table.addCell(cValue);
    }
}