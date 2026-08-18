package com.cms.billing.service;

import com.cms.billing.domain.entity.Contract;
import com.cms.billing.domain.entity.Invoice;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

@Service
public class PdfGeneratorService {

    public byte[] generateContractInvoicePdf(Contract contract, Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Invoice / Contract", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // blank line

            document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
            document.add(new Paragraph("Contract ID: " + contract.getId()));
            document.add(new Paragraph("Client ID: " + contract.getClientId()));
            document.add(new Paragraph("Account ID: " + contract.getAccountId()));
            document.add(new Paragraph("Date: " + contract.getStartDate()));
            document.add(new Paragraph("Due Date: " + invoice.getDueDate()));

            document.add(new Paragraph(" "));
            
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Financial Summary", boldFont));
            document.add(new Paragraph("Subtotal: " + invoice.getCurrency() + " " + invoice.getSubtotal()));
            document.add(new Paragraph("Tax Rate: " + invoice.getTaxRate() + "%"));
            document.add(new Paragraph("Tax Amount: " + invoice.getCurrency() + " " + invoice.getTaxAmount()));
            document.add(new Paragraph("Total Due: " + invoice.getCurrency() + " " + invoice.getTotalAmount(), boldFont));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Thank you for your business!"));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}
