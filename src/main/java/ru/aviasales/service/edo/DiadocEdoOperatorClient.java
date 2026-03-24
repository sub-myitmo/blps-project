package ru.aviasales.service.edo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Real Diadoc API client skeleton.
 * Activate via profile "diadoc-prod".
 *
 * Production usage requires:
 * - Diadoc API credentials (ddauth token or login/password)
 * - КриптоПро CSP on server for certificate-based operations
 * - Registered organization boxes in Diadoc
 *
 * Current implementation: throws UnsupportedOperationException.
 * This is intentional — it marks the exact boundary where
 * real integration work begins.
 */
@Component
@Profile("diadoc-prod")
public class DiadocEdoOperatorClient implements EdoOperatorClient {

    private final RestTemplate restTemplate;
    private final String diadocApiUrl;
    private final String diadocApiKey;

    public DiadocEdoOperatorClient(
            RestTemplate restTemplate,
            @Value("${edo.diadoc.api-url}") String diadocApiUrl,
            @Value("${edo.diadoc.api-key}") String diadocApiKey
    ) {
        this.restTemplate = restTemplate;
        this.diadocApiUrl = diadocApiUrl;
        this.diadocApiKey = diadocApiKey;
    }

    @Override
    public EdoSendResult sendDocumentForSigning(EdoDocumentPayload payload) {
        // Real implementation: POST /V3/PostMessage to Diadoc API
        // with document content, sender/recipient BoxIds, and signing request.
        // Returns MessageId + EntityId from Diadoc response.
        throw new UnsupportedOperationException(
                "Real Diadoc integration requires API credentials and КриптоПро setup. " +
                "Use profile 'diadoc-stub' for lab/testing."
        );
    }

    @Override
    public EdoSigningStatus getSigningStatus(String messageId, String entityId) {
        // Real implementation: GET /V3/GetMessage?messageId={}&entityId={}
        // Parse document status and signature metadata from response.
        throw new UnsupportedOperationException(
                "Real Diadoc integration not configured."
        );
    }

    @Override
    public EdoCounterSignResult initiateCounterSign(String messageId, String entityId,
                                                     String counterpartyBoxId) {
        // Real implementation: POST /V3/PostMessagePatch with RecipientSignature
        throw new UnsupportedOperationException(
                "Real Diadoc integration not configured."
        );
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "DiadocAuth ddauth_api_client_id=" + diadocApiKey);
        return headers;
    }
}
