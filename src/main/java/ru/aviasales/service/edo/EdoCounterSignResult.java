package ru.aviasales.service.edo;

public record EdoCounterSignResult(
        String status,
        String recipientCertThumbprint,
        String rawResponse
) {}
