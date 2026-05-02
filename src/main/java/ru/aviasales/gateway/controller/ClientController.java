package ru.aviasales.gateway.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_OWN')")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByClient(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(clientService.getCampaignsByClient(principal.getClientId()));
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('CAMPAIGN_CREATE')")
    public ResponseEntity<CampaignResponse> createCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = clientService.createCampaign(principal.getClientId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_UPDATE_OWN')")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignResponse response = clientService.updateCampaign(principal.getClientId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_DELETE_OWN')")
    public ResponseEntity<Void> deleteCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        clientService.deleteCampaign(principal.getClientId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAMPAIGN_SIGN_CLIENT','CAMPAIGN_PAUSE_OWN')")
    public ResponseEntity<CampaignResponse> actionWithCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ClientActionRequest request) {
        CampaignResponse response = clientService.actionCampaign(principal.getClientId(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_OWN')")
    public ResponseEntity<CampaignResponse> getCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        CampaignResponse response = clientService.getCampaign(principal.getClientId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/signature")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_OWN')")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getCampaignSignature(principal.getClientId(), id));
    }

    @GetMapping("/{id}/signature/pdf")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_OWN')")
    public ResponseEntity<Void> getCampaignSignaturePdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        String pdfUrl = clientService.getCampaignSignaturePdfUrl(principal.getClientId(), id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(pdfUrl))
                .build();
    }
}
