package ru.aviasales.service.edo;

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
}
