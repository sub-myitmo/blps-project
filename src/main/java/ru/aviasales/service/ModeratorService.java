package ru.aviasales.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.*;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.service.doc.*;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.service.dto.UpdateCampaignRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModeratorService {

    private final ModeratorRepository moderatorRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignDocumentPdfService campaignDocumentPdfService;

    public ModeratorService(
            ModeratorRepository moderatorRepository,
            AdvertisingCampaignRepository campaignRepository,
            CommentRepository commentRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignDocumentPdfService campaignDocumentPdfService
    ) {
        this.moderatorRepository = moderatorRepository;
        this.campaignRepository = campaignRepository;
        this.commentRepository = commentRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignDocumentPdfService = campaignDocumentPdfService;
    }

    @Transactional
    public CampaignResponse actionCampaign(String apiKey, Long campaignId,
                                             ModeratorActionRequest request) {
        Moderator moderator = getModeratorOrThrow(apiKey);

        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.PENDING) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Campaign must be in PENDING status for moderator signing");
                }
                validateConsentAccepted(request.getConsentAccepted());

                CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                        .orElseGet(() -> {
                            CampaignSignature newSignature = new CampaignSignature();
                            newSignature.setCampaign(campaign);
                            return newSignature;
                        });

                String documentSnapshot = CampaignSigningDocumentFactory.buildSnapshot(campaign);
                String documentHash = CampaignDocumentHashUtil.buildDocumentHash(documentSnapshot);
                signature.setDocumentHash(documentHash);
                signature.setHashAlgorithm(CampaignDocumentHashUtil.HASH_ALGORITHM);
                signature.setDocumentTemplateVersion(CampaignSigningDocumentFactory.TEMPLATE_VERSION);
                signature.setDocumentSnapshot(documentSnapshot);

                signature.setModeratorId(moderator.getId());
                signature.setModeratorSignedAtUtc(Instant.now());

                signature.setClientId(null);
                signature.setClientSignedAtUtc(null);
                signature.setFullySigned(false);
                signature.setFullySignedAtUtc(null);

                signature = campaignSignatureRepository.save(signature);
                campaign.setSignature(signature);
                campaign.transitionTo(CampaignStatus.AT_SIGNING);
                break;
            case REJECT:
                campaign.transitionTo(CampaignStatus.REJECTED);
                break;
            case PAUSE:
                campaign.transitionTo(CampaignStatus.PAUSED_BY_MODERATOR);
                break;
            default:
                throw new RuntimeException("Invalid action for moderation");
        }

        Comment comment = new Comment();
        comment.setModeratorId(moderator.getId());
        comment.setModerationComment(request.getComment());
        comment.setCampaign(campaign);
        commentRepository.save(comment);

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse updateCampaign(String apiKey, Long campaignId, UpdateCampaignRequest request) {
        getModeratorOrThrow(apiKey);
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        LocalDate startDate = request.getStartDate() == null ? campaign.getStartDate() : request.getStartDate();
        LocalDate endDate = request.getEndDate() == null ? campaign.getEndDate() : request.getEndDate();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before end date");
        }

        campaign.setName(request.getName() == null ? campaign.getName() : request.getName());
        campaign.setContent(request.getContent() == null ? campaign.getContent() : request.getContent());
        campaign.setTargetUrl(request.getTargetUrl() == null ? campaign.getTargetUrl() : request.getTargetUrl());
        campaign.setDailyBudget(request.getDailyBudget() == null ? campaign.getDailyBudget() : request.getDailyBudget());
        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public void deleteCampaign(String apiKey, Long campaignId) {
        getModeratorOrThrow(apiKey);
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a completed campaign");
        }

        campaignRepository.delete(campaign);
    }

    @Transactional
    public void deleteComment(String apiKey, Long commentId) {
        Moderator moderator = getModeratorOrThrow(apiKey);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getModeratorId().equals(moderator.getId())) {
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

    @Transactional(readOnly = true)
    public byte[] getCampaignSignaturePdf(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));
        return campaignDocumentPdfService.generatePdf(signature);
    }

    public List<CampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        List<AdvertisingCampaign> campaigns = campaignRepository.findByStatusWithDetails(status);
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Moderator getModeratorOrThrow(String apiKey) {
        return moderatorRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderator not found"));
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
        if (Boolean.FALSE.equals(consentAccepted)) {
            throw new RuntimeException("Explicit electronic-signature consent is required");
        }
    }
}
