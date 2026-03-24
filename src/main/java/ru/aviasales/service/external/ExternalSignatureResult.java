package ru.aviasales.service.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSignatureResult {
    private String signatureToken;
    private String provider;
    private String signedAt;
}
