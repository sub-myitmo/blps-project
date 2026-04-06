package ru.aviasales.service.doc;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.aviasales.dal.model.CampaignSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CampaignDocumentPdfService {

    private static final float MARGIN = 50f;
    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_BODY = 11f;
    private static final float FONT_SIZE_FOOTER = 9f;
    private static final float LINE_HEIGHT_TITLE = 24f;
    private static final float LINE_HEIGHT_BODY = 18f;
    private static final float LINE_HEIGHT_FOOTER = 14f;
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z").withZone(ZoneId.of("UTC"));

    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();

    static {
        FIELD_LABELS.put("campaignId", "ID кампании");
        FIELD_LABELS.put("campaignName", "Название кампании");
        FIELD_LABELS.put("content", "Содержание");
        FIELD_LABELS.put("targetUrl", "Целевой URL");
        FIELD_LABELS.put("dailyBudget", "Дневной бюджет");
        FIELD_LABELS.put("startDate", "Дата начала");
        FIELD_LABELS.put("endDate", "Дата окончания");
        FIELD_LABELS.put("clientId", "ID клиента");
    }

    public byte[] generatePdf(CampaignSignature signature) {
        try (PDDocument doc = new PDDocument();
             InputStream fontStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {

            PDType0Font font = PDType0Font.load(doc, fontStream);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float contentWidth = pageWidth - 2 * MARGIN;

            List<String[]> bodyLines = buildBodyLines(signature);
            List<String> footerLines = buildFooterLines(signature);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = pageHeight - MARGIN;

                // Title
                cs.beginText();
                cs.setFont(font, FONT_SIZE_TITLE);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Документ подписания рекламной кампании");
                cs.endText();
                y -= LINE_HEIGHT_TITLE * 1.5f;

                // Separator line
                cs.moveTo(MARGIN, y + 4);
                cs.lineTo(MARGIN + contentWidth, y + 4);
                cs.stroke();
                y -= LINE_HEIGHT_BODY;

                // Body fields
                cs.setFont(font, FONT_SIZE_BODY);
                for (String[] labelValue : bodyLines) {
                    if (y < MARGIN + footerLines.size() * LINE_HEIGHT_FOOTER + 40) {
                        cs.endText();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        pageHeight = page.getMediaBox().getHeight();
                        cs.beginText();
                        cs.setFont(font, FONT_SIZE_BODY);
                        y = pageHeight - MARGIN;
                    }
                    cs.beginText();
                    cs.setFont(font, FONT_SIZE_BODY);
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(labelValue[0] + ": " + labelValue[1]);
                    cs.endText();
                    y -= LINE_HEIGHT_BODY;
                }

                // Footer separator
                float footerTop = MARGIN + footerLines.size() * LINE_HEIGHT_FOOTER + 8;
                cs.moveTo(MARGIN, footerTop);
                cs.lineTo(MARGIN + contentWidth, footerTop);
                cs.stroke();

                // Footer lines
                float fy = footerTop - LINE_HEIGHT_FOOTER;
                for (String line : footerLines) {
                    cs.beginText();
                    cs.setFont(font, FONT_SIZE_FOOTER);
                    cs.newLineAtOffset(MARGIN, fy);
                    cs.showText(line);
                    cs.endText();
                    fy -= LINE_HEIGHT_FOOTER;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF for campaign signature " + signature.getId(), e);
        }
    }

    private List<String[]> buildBodyLines(CampaignSignature signature) {
        Map<String, String> snapshotFields = parseSnapshot(signature.getDocumentSnapshot());
        List<String[]> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : FIELD_LABELS.entrySet()) {
            String raw = snapshotFields.getOrDefault(entry.getKey(), "");
            lines.add(new String[]{entry.getValue(), raw.isBlank() ? "—" : raw});
        }
        return lines;
    }

    private List<String> buildFooterLines(CampaignSignature signature) {
        List<String> lines = new ArrayList<>();
        lines.add("Версия шаблона: " + signature.getDocumentTemplateVersion());
        lines.add("Алгоритм хэша: " + signature.getHashAlgorithm());
        lines.add("Хэш документа: " + signature.getDocumentHash());
        if (signature.getModeratorId() != null) {
            String ts = signature.getModeratorSignedAtUtc() != null
                    ? TIMESTAMP_FMT.format(signature.getModeratorSignedAtUtc()) : "—";
            lines.add("Подписан модератором ID=" + signature.getModeratorId() + " в " + ts);
        }
        if (signature.getClientId() != null) {
            String ts = signature.getClientSignedAtUtc() != null
                    ? TIMESTAMP_FMT.format(signature.getClientSignedAtUtc()) : "—";
            lines.add("Подписан клиентом ID=" + signature.getClientId() + " в " + ts);
        }
        return lines;
    }

    private Map<String, String> parseSnapshot(String snapshot) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (snapshot == null) return fields;
        for (String line : snapshot.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                fields.put(line.substring(0, eq).trim(), line.substring(eq + 1));
            }
        }
        return fields;
    }
}
