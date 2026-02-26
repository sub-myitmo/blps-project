package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.service.ModeratorService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moderator/campaigns")
@RequiredArgsConstructor
public class ModeratorController {

    private final ModeratorService moderatorService;
    private final AdvertisingCampaignRepository campaignRepository;

    @GetMapping("/{status}")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByStatus(@PathVariable AdvertisingCampaign.CampaignStatus status) {
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByStatus(status);
        List<CampaignResponse> responses = campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<CampaignResponse> moderateCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody ModeratorActionRequest request) {
        CampaignResponse response = moderatorService.actionCampaign(apiKey, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/signature")
    public ResponseEntity<CampaignResponse> signatureCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        CampaignResponse response = moderatorService.signatureCampaign(apiKey, id);
        return ResponseEntity.ok(response);
    }
}
