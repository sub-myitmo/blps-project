package ru.aviasales.service.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSignatureRequest {
    private String documentHash;
    private String hashAlgorithm;
    private String eventType;
    private Long actorId;
    private String actorName;
    private String consentStatement;
    private String occurredAtUtc;
}
