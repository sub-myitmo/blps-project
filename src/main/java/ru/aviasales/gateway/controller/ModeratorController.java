package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.service.ModeratorService;

import java.util.List;

@RestController
@RequestMapping("/api/moderator/campaigns")
@RequiredArgsConstructor
public class ModeratorController {

    private final ModeratorService moderatorService;

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByStatus(@PathVariable CampaignStatus status) {
        return ResponseEntity.ok(moderatorService.getCampaignsByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(moderatorService.getCampaign(id));
    }

    @PostMapping("/{id}")
    public ResponseEntity<CampaignResponse> actionWithCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody ModeratorActionRequest request) {
        CampaignResponse response = moderatorService.actionCampaign(apiKey, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/signature")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(@PathVariable Long id) {
        return ResponseEntity.ok(moderatorService.getCampaignSignature(id));
    }
}
