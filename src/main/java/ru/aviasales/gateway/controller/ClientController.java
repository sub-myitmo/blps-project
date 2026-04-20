package ru.aviasales.gateway.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.*;
import ru.aviasales.service.ClientService;

import java.util.List;

@RestController
@RequestMapping("/api/client/campaigns")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByClient(
            @RequestHeader("Authorization") String apiKey) {
        return ResponseEntity.ok(clientService.getCampaignsByClient(apiKey));
    }

    @PostMapping("")
    public ResponseEntity<CampaignResponse> createCampaign(
            @RequestHeader("Authorization") String apiKey,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = clientService.createCampaign(apiKey, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignResponse response = clientService.updateCampaign(apiKey, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        clientService.deleteCampaign(apiKey, id);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/{id}/signature/pdf")
    public ResponseEntity<byte[]> getCampaignSignaturePdf(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        byte[] pdf = clientService.getCampaignSignaturePdf(apiKey, id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("campaign-" + id + "-signing-document.pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
