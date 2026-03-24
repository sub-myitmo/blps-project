package ru.aviasales.service.edo;

public record EdoSendResult(
        String messageId,
        String entityId,
        String operator,
        String status,
        String senderCertThumbprint,
        String rawResponse
) {}
