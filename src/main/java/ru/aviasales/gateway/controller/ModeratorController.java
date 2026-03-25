package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.service.ModeratorService;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}/signature/document.pdf")
    public ResponseEntity<byte[]> downloadFrozenDocumentPdf(@PathVariable Long id) {
        byte[] pdf = moderatorService.getFrozenDocumentPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"campaign-" + id + "-document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/signature/artifact")
    public ResponseEntity<?> downloadSignatureArtifact(@PathVariable Long id) {
        CampaignSignature signature = moderatorService.getSignatureWithArtifactSync(id);
        if (signature.getSignatureArtifact() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Signature artifact not available",
                            "reason", "The ЭДО operator has not provided a downloadable signature artifact. " +
                                    "Only signature metadata/evidence is currently available."
                    ));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + signature.getSignatureArtifactFilename() + "\"")
                .contentType(MediaType.parseMediaType(signature.getSignatureArtifactContentType()))
                .body(signature.getSignatureArtifact());
    }
}
