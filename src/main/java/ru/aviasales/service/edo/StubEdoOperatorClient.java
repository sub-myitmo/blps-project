package ru.aviasales.service.edo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Lab-only stub that simulates a Diadoc-like ЭДО operator.
 * Returns plausible identifiers and immediate success statuses.
 * Activated by default (primary) or via profile "diadoc-stub".
 */
@Component
@Profile("diadoc-stub")
public class StubEdoOperatorClient implements EdoOperatorClient {

    private static final String OPERATOR = "DIADOC_STUB";
    private static final String STUB_CERT_THUMBPRINT = "STUB_CERT_" + "A1B2C3D4E5F6";

    @Override
    public EdoSendResult sendDocumentForSigning(EdoDocumentPayload payload) {
        String messageId = "msg-" + UUID.randomUUID();
        String entityId = "ent-" + UUID.randomUUID();

        String rawResponse = String.format(
                "{\"MessageId\":\"%s\",\"EntityId\":\"%s\",\"Status\":\"SendersSignatureIsRequired\"}",
                messageId, entityId
        );

        return new EdoSendResult(
                messageId,
                entityId,
                OPERATOR,
                "SENDER_SIGNED",
                STUB_CERT_THUMBPRINT,
                rawResponse
        );
    }

    @Override
    public EdoSigningStatus getSigningStatus(String messageId, String entityId) {
        return new EdoSigningStatus(
                "COMPLETED",
                STUB_CERT_THUMBPRINT,
                STUB_CERT_THUMBPRINT,
                Instant.now().minusSeconds(60),
                Instant.now(),
                "{\"Status\":\"Completed\"}"
        );
    }

    @Override
    public EdoCounterSignResult initiateCounterSign(String messageId, String entityId,
                                                     String counterpartyBoxId) {
        return new EdoCounterSignResult(
                "RECIPIENT_SIGNED",
                STUB_CERT_THUMBPRINT,
                "{\"Status\":\"RecipientSignatureApplied\"}"
        );
    }
}
