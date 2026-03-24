package ru.aviasales.service.edo;

public record EdoDocumentPayload(
        String documentContent,
        String documentHash,
        String senderBoxId,
        String recipientBoxId,
        String documentTitle
) {}
