package ru.aviasales.gateway.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.CampaignRequest;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.ClientService;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ClientActionRequest;

@RestController
@RequestMapping("/api/client/campaigns")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("")
    public ResponseEntity<CampaignResponse> createCampaign(
            @RequestHeader("Authorization") String apiKey,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = clientService.createCampaign(apiKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}")
    public ResponseEntity<CampaignResponse> actionWithCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody ClientActionRequest request) {
        CampaignResponse response = clientService.actionCampaign(apiKey, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        CampaignResponse response = clientService.getCampaign(apiKey, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/signature")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getCampaignSignature(apiKey, id));
    }

    @GetMapping("/{id}/signature/document.pdf")
    public ResponseEntity<byte[]> downloadFrozenDocumentPdf(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        byte[] pdf = clientService.getFrozenDocumentPdf(apiKey, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"campaign-" + id + "-document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
