package ru.aviasales.service.doc;

import ru.aviasales.dal.model.AdvertisingCampaign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CampaignDocumentHashUtil {
    public static final String HASH_ALGORITHM = "SHA-256";

    private CampaignDocumentHashUtil() {
    }

    public static String buildDocumentHash(AdvertisingCampaign campaign) {
        return buildDocumentHash(CampaignSigningDocumentFactory.buildSnapshot(campaign));
    }

    public static String buildDocumentHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot build campaign document hash");
        }
    }
}
