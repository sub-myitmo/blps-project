package ru.aviasales.service.edo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lab-only stub that simulates a Diadoc-like ЭДО operator.
 * It keeps a small in-memory state machine so initiation and confirmation are not conflated.
 * Activated for every profile except "diadoc-prod".
 */
@Component
@Profile("!diadoc-prod")
public class StubEdoOperatorClient implements EdoOperatorClient {

    private static final String OPERATOR = "DIADOC_STUB";
    private static final String STUB_CERT_THUMBPRINT = "STUB_CERT_" + "A1B2C3D4E5F6";
    private final Map<String, StubDocumentState> documents = new ConcurrentHashMap<>();

    @Override
    public EdoSendResult sendDocumentForSigning(EdoDocumentPayload payload) {
        String messageId = "msg-" + UUID.randomUUID();
        String entityId = "ent-" + UUID.randomUUID();
        Instant senderSignedAt = Instant.now();
        documents.put(key(messageId, entityId), new StubDocumentState(
                EdoDocumentStatus.RECIPIENT_SIGNATURE_REQUIRED,
                senderSignedAt,
                null
        ));

        String rawResponse = String.format(
                "{\"MessageId\":\"%s\",\"EntityId\":\"%s\",\"Status\":\"%s\"}",
                messageId, entityId, EdoDocumentStatus.RECIPIENT_SIGNATURE_REQUIRED
        );

        return new EdoSendResult(
                messageId,
                entityId,
                OPERATOR,
                EdoDocumentStatus.RECIPIENT_SIGNATURE_REQUIRED,
                STUB_CERT_THUMBPRINT,
                rawResponse
        );
    }

    @Override
    public EdoSigningStatus getSigningStatus(String messageId, String entityId) {
        StubDocumentState documentState = getDocumentState(messageId, entityId);
        if (EdoDocumentStatus.COUNTERSIGN_INITIATED.equals(documentState.status())) {
            documentState = documentState.complete();
            documents.put(key(messageId, entityId), documentState);
        }

        return new EdoSigningStatus(
                documentState.status(),
                STUB_CERT_THUMBPRINT,
                documentState.recipientSignedAt() != null ? STUB_CERT_THUMBPRINT : null,
                documentState.senderSignedAt(),
                documentState.recipientSignedAt(),
                "{\"Status\":\"" + documentState.status() + "\"}"
        );
    }

    @Override
    public EdoCounterSignResult initiateCounterSign(String messageId, String entityId,
                                                     String counterpartyBoxId) {
        StubDocumentState currentState = getDocumentState(messageId, entityId);
        if (!EdoDocumentStatus.canInitiateCountersign(currentState.status())) {
            throw new IllegalStateException("Stub document is not ready for countersign");
        }

        documents.put(key(messageId, entityId), currentState.initiateCountersign());
        return new EdoCounterSignResult(
                EdoDocumentStatus.COUNTERSIGN_INITIATED,
                null,
                "{\"Status\":\"" + EdoDocumentStatus.COUNTERSIGN_INITIATED + "\"}"
        );
    }

    private StubDocumentState getDocumentState(String messageId, String entityId) {
        StubDocumentState state = documents.get(key(messageId, entityId));
        if (state == null) {
            throw new IllegalStateException("Stub document not found");
        }
        return state;
    }

    private String key(String messageId, String entityId) {
        return messageId + ":" + entityId;
    }

    private record StubDocumentState(
            String status,
            Instant senderSignedAt,
            Instant recipientSignedAt
    ) {
        StubDocumentState initiateCountersign() {
            return new StubDocumentState(EdoDocumentStatus.COUNTERSIGN_INITIATED, senderSignedAt, recipientSignedAt);
        }

        StubDocumentState complete() {
            return new StubDocumentState(EdoDocumentStatus.COMPLETED, senderSignedAt, Instant.now());
        }
    }
}
