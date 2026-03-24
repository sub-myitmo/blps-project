package ru.aviasales.service.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpExternalSignatureService implements ExternalSignatureService {

    private final RestTemplate restTemplate;
    private final String signatureServiceUrl;
    private final String apiKey;

    public HttpExternalSignatureService(
            RestTemplate restTemplate,
            @Value("${signature.service.url}") String signatureServiceUrl,
            @Value("${signature.service.api-key}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.signatureServiceUrl = signatureServiceUrl;
        this.apiKey = apiKey;
    }

    @Override
    public ExternalSignatureResult sign(ExternalSignatureRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<ExternalSignatureRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForObject(signatureServiceUrl + "/signatures", entity, ExternalSignatureResult.class);
    }
}
