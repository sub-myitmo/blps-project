package ru.aviasales.gateway.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.CampaignRequest;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.ClientService;

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

    @PostMapping("/{id}/pause")
    public ResponseEntity<CampaignResponse> pauseCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        CampaignResponse response = clientService.pauseCampaign(apiKey, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<CampaignResponse> resumeCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        CampaignResponse response = clientService.activeCampaign(apiKey, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/signature")
    public ResponseEntity<Void> signatureCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        // логика по подписи документов
        return null;
    }
}
