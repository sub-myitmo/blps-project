package ru.aviasales.service;

import ru.aviasales.dal.model.AdvertisingCampaign;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class CampaignDocumentHashUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CampaignDocumentHashUtil() {
    }

    public static String buildDocumentHash(AdvertisingCampaign campaign) {
        String payload = String.join("|",
                value(campaign.getName()),
                value(campaign.getContent()),
                value(campaign.getTargetUrl()),
                value(campaign.getDailyBudget()),
                value(campaign.getStartDate()),
                value(campaign.getEndDate()),
                value(campaign.getClient() != null ? campaign.getClient().getId() : null)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot build campaign document hash");
        }
    }

    private static String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        return value.toString();
    }
}
