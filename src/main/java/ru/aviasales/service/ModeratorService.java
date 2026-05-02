package ru.aviasales.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.*;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.security.AuthorityCheck;
import ru.aviasales.security.PrivilegeCodes;
import ru.aviasales.service.doc.*;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.CommentRequest;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.security.UserPrincipal;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModeratorService {

    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignSigningSagaService campaignSigningSagaService;
    private final PdfStorageService pdfStorageService;

    public ModeratorService(
            AdvertisingCampaignRepository campaignRepository,
            CommentRepository commentRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignSigningSagaService campaignSigningSagaService,
            PdfStorageService pdfStorageService
    ) {
        this.campaignRepository = campaignRepository;
        this.commentRepository = commentRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignSigningSagaService = campaignSigningSagaService;
        this.pdfStorageService = pdfStorageService;
    }

    @Transactional
    public CampaignResponse actionCampaign(UserPrincipal principal, Long campaignId,
                                             ModeratorActionRequest request) {
        AuthorityCheck.require(requiredPrivilegeFor(request.getAction()));

        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.PENDING) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Campaign must be in PENDING status for moderator signing");
                }
                validateConsentAccepted(request.getConsentAccepted());
                campaignSigningSagaService.restartSigning(campaign, principal.getModeratorId());
                campaign.transitionTo(CampaignStatus.AT_SIGNING);
                break;
            case REJECT:
                campaign.transitionTo(CampaignStatus.REJECTED);
                break;
            case PAUSE:
                campaign.transitionTo(CampaignStatus.PAUSED_BY_MODERATOR);
                break;
            default:
                throw new IllegalArgumentException("Invalid action for moderation");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse addComment(UserPrincipal principal, Long campaignId, CommentRequest request) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        Comment comment = new Comment();
        comment.setModeratorId(principal.getModeratorId());
        comment.setModerationComment(request.getComment());
        comment.setCampaign(campaign);
        comment = commentRepository.save(comment);
        campaign.getComments().add(comment);

        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional
    public void deleteCampaign(Long campaignId) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a completed campaign");
        }

        // Block hard-delete of signed campaigns to preserve audit trail.
        CampaignSignature signature = campaign.getSignature();
        if (signature != null && signature.isFullySigned()
                && campaign.getStatus() != CampaignStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a fully-signed campaign; use REJECT or PAUSE");
        }

        campaignRepository.delete(campaign);
    }

    @Transactional
    public void deleteComment(UserPrincipal principal, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean canDeleteAny = AuthorityCheck.hasAuthority(PrivilegeCodes.COMMENT_DELETE_ANY);
        if (!canDeleteAny && (comment.getModeratorId() == null
                || !comment.getModeratorId().equals(principal.getModeratorId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another moderator's comment");
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));
        return CampaignSignatureDetailsResponse.fromEntity(signature);
    }

    @Transactional
    public String getCampaignSignaturePdfUrl(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));
        signature = campaignSigningSagaService.ensurePdfStored(signature);
        return pdfStorageService.buildPresignedUrl(signature.getPdfObjectKey());
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {
        List<AdvertisingCampaign> campaigns = campaignRepository.findAllWithDetails();
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        List<AdvertisingCampaign> campaigns = campaignRepository.findByStatusWithDetails(status);
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private AdvertisingCampaign getCampaignOrThrow(Long campaignId) {
        return campaignRepository.findByIdWithDetails(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private AdvertisingCampaign getCampaignForActionOrThrow(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private void validateConsentAccepted(Boolean consentAccepted) {
        if (!Boolean.TRUE.equals(consentAccepted)) {
            throw new IllegalArgumentException("Explicit electronic-signature consent is required");
        }
    }

    private String requiredPrivilegeFor(ModeratorActionRequest.Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action is required");
        }
        return switch (action) {
            case SIGN_DOC -> PrivilegeCodes.CAMPAIGN_MODERATE_SIGN;
            case REJECT -> PrivilegeCodes.CAMPAIGN_REJECT;
            case PAUSE -> PrivilegeCodes.CAMPAIGN_PAUSE_ANY;
        };
    }

}
