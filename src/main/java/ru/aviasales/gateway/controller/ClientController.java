package ru.aviasales.gateway.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.*;
import ru.aviasales.service.ClientService;
import ru.aviasales.security.UserPrincipal;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/client/campaigns")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByClient(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clientService.getCampaignsByClient(principal.getClientId()));
    }

    @PostMapping("")
    public ResponseEntity<CampaignResponse> createCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = clientService.createCampaign(principal.getClientId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignResponse response = clientService.updateCampaign(principal.getClientId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        clientService.deleteCampaign(principal.getClientId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}")
    public ResponseEntity<CampaignResponse> actionWithCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ClientActionRequest request) {
        CampaignResponse response = clientService.actionCampaign(principal.getClientId(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        CampaignResponse response = clientService.getCampaign(principal.getClientId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/signature")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getCampaignSignature(principal.getClientId(), id));
    }

    @GetMapping("/{id}/signature/pdf")
    public ResponseEntity<Void> getCampaignSignaturePdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        String pdfUrl = clientService.getCampaignSignaturePdfUrl(principal.getClientId(), id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(pdfUrl))
                .build();
    }
}
