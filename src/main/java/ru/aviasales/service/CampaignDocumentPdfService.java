package ru.aviasales.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import ru.aviasales.dal.model.CampaignSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class CampaignDocumentPdfService {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 16;
    private static final float FONT_SIZE_BODY = 10;
    private static final float FONT_SIZE_SMALL = 8;
    private static final float LINE_HEIGHT = 14;
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] generateFrozenDocumentPdf(CampaignSignature signature) {
        if (signature.getDocumentSnapshot() == null || signature.getDocumentSnapshot().isBlank()) {
            throw new IllegalStateException("Cannot generate PDF: document snapshot is empty");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            float pageWidth = page.getMediaBox().getWidth();
            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            float contentWidth = pageWidth - 2 * MARGIN;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // title
                yPosition = drawText(cs, "Frozen Campaign Document", fontBold, FONT_SIZE_TITLE,
                        MARGIN, yPosition);
                yPosition -= LINE_HEIGHT;

                // disclaimer
                yPosition = drawText(cs, "This document is a visual representation of the frozen campaign snapshot.",
                        fontItalic, FONT_SIZE_SMALL, MARGIN, yPosition);
                yPosition = drawText(cs, "It does not constitute a cryptographic or legally qualified electronic signature.",
                        fontItalic, FONT_SIZE_SMALL, MARGIN, yPosition);
                yPosition -= LINE_HEIGHT;

                // separator
                yPosition = drawLine(cs, MARGIN, yPosition, MARGIN + contentWidth, yPosition);
                yPosition -= LINE_HEIGHT;

                // snapshot fields
                String[] lines = signature.getDocumentSnapshot().split("\n");
                for (String line : lines) {
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = formatKey(line.substring(0, eqIdx));
                        String value = line.substring(eqIdx + 1);
                        if (value.isEmpty()) {
                            value = "(not specified)";
                        }
                        yPosition = drawLabelValue(cs, key, value, fontBold, fontRegular,
                                FONT_SIZE_BODY, MARGIN, yPosition, contentWidth);
                    } else {
                        yPosition = drawText(cs, line, fontRegular, FONT_SIZE_BODY, MARGIN, yPosition);
                    }
                }

                yPosition -= LINE_HEIGHT;
                yPosition = drawLine(cs, MARGIN, yPosition, MARGIN + contentWidth, yPosition);
                yPosition -= LINE_HEIGHT;

                // integrity metadata
                yPosition = drawText(cs, "Integrity metadata", fontBold, FONT_SIZE_BODY, MARGIN, yPosition);
                yPosition = drawLabelValue(cs, "Hash algorithm", safe(signature.getHashAlgorithm()),
                        fontBold, fontRegular, FONT_SIZE_BODY, MARGIN, yPosition, contentWidth);
                yPosition = drawLabelValue(cs, "Document hash", safe(signature.getDocumentHash()),
                        fontBold, fontRegular, FONT_SIZE_BODY, MARGIN, yPosition, contentWidth);
                yPosition = drawLabelValue(cs, "Template version", safe(signature.getDocumentTemplateVersion()),
                        fontBold, fontRegular, FONT_SIZE_BODY, MARGIN, yPosition, contentWidth);

                if (signature.getModeratorSignedAtUtc() != null) {
                    yPosition -= LINE_HEIGHT;
                    yPosition = drawLabelValue(cs, "Frozen at",
                            UTC_FORMATTER.format(signature.getModeratorSignedAtUtc()),
                            fontBold, fontRegular, FONT_SIZE_BODY, MARGIN, yPosition, contentWidth);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private float drawText(PDPageContentStream cs, String text, PDType1Font font,
                           float fontSize, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private float drawLabelValue(PDPageContentStream cs, String label, String value,
                                 PDType1Font boldFont, PDType1Font regularFont,
                                 float fontSize, float x, float y, float contentWidth) throws IOException {
        String labelStr = label + ": ";
        float labelWidth = boldFont.getStringWidth(labelStr) / 1000 * fontSize;

        cs.beginText();
        cs.setFont(boldFont, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(labelStr));
        cs.setFont(regularFont, fontSize);

        String sanitizedValue = sanitize(value);
        float availableWidth = contentWidth - labelWidth;
        float valueWidth = regularFont.getStringWidth(sanitizedValue) / 1000 * fontSize;
        if (valueWidth > availableWidth && sanitizedValue.length() > 10) {
            int maxChars = (int) (sanitizedValue.length() * (availableWidth / valueWidth));
            maxChars = Math.max(10, Math.min(maxChars, sanitizedValue.length()));
            sanitizedValue = sanitizedValue.substring(0, maxChars) + "...";
        }
        cs.showText(sanitizedValue);
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private float drawLine(PDPageContentStream cs, float x1, float y1,
                           float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        return y1;
    }

    private String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c <= 0xFF) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private String formatKey(String rawKey) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawKey.length(); i++) {
            char c = rawKey.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value == null ? "(not specified)" : value;
    }
}
