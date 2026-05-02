package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.CommentRequest;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.service.ModeratorService;
import ru.aviasales.security.UserPrincipal;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/manager/campaigns", "/api/moderator/campaigns"})
@RequiredArgsConstructor
public class ModeratorController {

    private final ModeratorService moderatorService;

    @GetMapping("")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(moderatorService.getAllCampaigns());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByStatus(@PathVariable CampaignStatus status) {
        return ResponseEntity.ok(moderatorService.getCampaignsByStatus(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(moderatorService.getCampaign(id));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAMPAIGN_MODERATE_SIGN','CAMPAIGN_REJECT','CAMPAIGN_PAUSE_ANY')")
    public ResponseEntity<CampaignResponse> actionWithCampaign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ModeratorActionRequest request) {
        CampaignResponse response = moderatorService.actionCampaign(principal, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    public ResponseEntity<CampaignResponse> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(moderatorService.addComment(principal, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAMPAIGN_DELETE_ANY')")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable Long id) {
        moderatorService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_DELETE_OWN') or hasAuthority('COMMENT_DELETE_ANY')")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId) {
        moderatorService.deleteComment(principal, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/signature")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<CampaignSignatureDetailsResponse> getCampaignSignature(@PathVariable Long id) {
        return ResponseEntity.ok(moderatorService.getCampaignSignature(id));
    }

    @GetMapping("/{id}/signature/pdf")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<Void> getCampaignSignaturePdf(@PathVariable Long id) {
        String pdfUrl = moderatorService.getCampaignSignaturePdfUrl(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(pdfUrl))
                .build();
    }
}
