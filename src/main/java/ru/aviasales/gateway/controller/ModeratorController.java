package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.service.dto.UpdateCampaignRequest;
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

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        return ResponseEntity.ok(moderatorService.updateCampaign(apiKey, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long id) {
        moderatorService.deleteCampaign(apiKey, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("Authorization") String apiKey,
            @PathVariable Long commentId) {
        moderatorService.deleteComment(apiKey, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/signature")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(@PathVariable Long id) {
        return ResponseEntity.ok(moderatorService.getCampaignSignature(id));
    }

    @GetMapping("/{id}/signature/pdf")
    public ResponseEntity<byte[]> getCampaignSignaturePdf(@PathVariable Long id) {
        byte[] pdf = moderatorService.getCampaignSignaturePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("campaign-" + id + "-signing-document.pdf")
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
