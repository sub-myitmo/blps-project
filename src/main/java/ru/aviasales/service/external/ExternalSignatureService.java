package ru.aviasales.service.external;

public interface ExternalSignatureService {
    ExternalSignatureResult sign(ExternalSignatureRequest request);
}
