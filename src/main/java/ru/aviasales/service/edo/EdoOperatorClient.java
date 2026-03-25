package ru.aviasales.service.edo;

import java.util.Optional;

/**
 * Integration boundary with an ЭДО (electronic document exchange) operator.
 * The backend delegates legal signing responsibility to the operator.
 * The backend NEVER creates qualified electronic signatures itself.
 * Implementations must map provider-specific states into canonical backend statuses
 * defined in {@link EdoDocumentStatus}. Raw provider payloads should be preserved separately.
 */
public interface EdoOperatorClient {

    /** Upload frozen document and initiate signing by first party (sender / moderator's org). */
    EdoSendResult sendDocumentForSigning(EdoDocumentPayload payload);

    /** Query current signing status from operator side. */
    EdoSigningStatus getSigningStatus(String messageId, String entityId);

    /** Initiate countersign by second party (recipient / client's org). */
    EdoCounterSignResult initiateCounterSign(String messageId, String entityId, String counterpartyBoxId);

    /**
     * Fetch the real signature artifact (e.g. CMS/PKCS#7 detached signature,
     * signed PDF, or other operator-issued file) for a completed document.
     *
     * Returns {@link Optional#empty()} if the operator does not support artifact
     * retrieval, or if the document is not yet fully signed.
     */
    default Optional<EdoSignatureArtifact> fetchSignatureArtifact(String messageId, String entityId) {
        return Optional.empty();
    }
}
