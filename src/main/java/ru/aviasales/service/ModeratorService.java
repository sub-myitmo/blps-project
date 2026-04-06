package ru.aviasales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.*;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.service.doc.*;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;

import java.time.Instant;
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
        Moderator moderator = moderatorRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.PENDING) {
                    throw new RuntimeException("Campaign must be in PENDING status for moderator signing");
                }
                validateConsentAccepted(request.getConsentAccepted());

                CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                        .orElseGet(() -> {
                            CampaignSignature newSignature = new CampaignSignature();
                            newSignature.setCampaign(campaign);
                            return newSignature;
                        });

                // Build and freeze document
                String documentSnapshot = CampaignSigningDocumentFactory.buildSnapshot(campaign);
                String documentHash = CampaignDocumentHashUtil.buildDocumentHash(documentSnapshot);
                signature.setDocumentHash(documentHash);
                signature.setHashAlgorithm(CampaignDocumentHashUtil.HASH_ALGORITHM);
                signature.setDocumentTemplateVersion(CampaignSigningDocumentFactory.TEMPLATE_VERSION);
                signature.setDocumentSnapshot(documentSnapshot);

                // Record moderator signature
                signature.setModeratorId(moderator.getId());
                signature.setModeratorSignedAtUtc(Instant.now());

                // Reset client signing state for fresh cycle
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
        comment.setModerator(moderator);
        comment.setModerationComment(request.getComment());
        comment.setCampaign(campaign);
        commentRepository.save(comment);

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));
        return CampaignSignatureDetailsResponse.fromEntity(signature);
    }

    @Transactional(readOnly = true)
    public byte[] getCampaignSignaturePdf(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));
        return campaignDocumentPdfService.generatePdf(signature);
    }

    public List<CampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByStatus(status);
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private AdvertisingCampaign getCampaignOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    private AdvertisingCampaign getCampaignForActionOrThrow(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    private void validateConsentAccepted(Boolean consentAccepted) {
        if (Boolean.FALSE.equals(consentAccepted)) {
            throw new RuntimeException("Explicit electronic-signature consent is required");
        }
    }
}
