package ru.aviasales.service.edo;

import java.time.Instant;

public record EdoSigningStatus(
        String status,
        String senderCertThumbprint,
        String recipientCertThumbprint,
        Instant senderSignedAt,
        Instant recipientSignedAt,
        String rawResponse
) {}
