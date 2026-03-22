package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.*;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModeratorService {

    private final ModeratorRepository moderatorRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignSignatureAuditService campaignSignatureAuditService;

    @Transactional
    public CampaignResponse actionCampaign(String apiKey, Long campaignId,
                                             ModeratorActionRequest request) {
        Moderator moderator = moderatorRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);

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

                String documentSnapshot = CampaignSigningDocumentFactory.buildSnapshot(campaign);
                signature.setDocumentHash(CampaignDocumentHashUtil.buildDocumentHash(documentSnapshot));
                signature.setHashAlgorithm(CampaignDocumentHashUtil.HASH_ALGORITHM);
                signature.setSignatureType(SignatureType.SIMPLE_ELECTRONIC_SIGNATURE);
                signature.setDocumentTemplateVersion(CampaignSigningDocumentFactory.TEMPLATE_VERSION);
                signature.setDocumentSnapshot(documentSnapshot);
                signature.getAuditEvents().clear();
                signature.setModeratorId(moderator.getId());
                signature.setModeratorSignedAt(null);
                signature.setModeratorSignedAtUtc(null);
                signature.setModeratorEvidence(null);
                signature.setClientId(null);
                signature.setClientSignedAt(null);
                signature.setClientSignedAtUtc(null);
                signature.setClientEvidence(null);
                signature.setFullySigned(false);
                signature.setFullySignedAtUtc(null);

                signature = campaignSignatureRepository.save(signature);
                CampaignSignatureAuditService.SignatureCapture moderatorCapture =
                        campaignSignatureAuditService.captureSignature(
                                signature,
                                CampaignSignatureEventType.MODERATOR_SIGNED,
                                SignatureActorType.MODERATOR,
                                moderator.getId(),
                                moderator.getName(),
                                CampaignSignaturePolicy.MODERATOR_CONSENT_STATEMENT
                        );
                signature.setModeratorSignedAt(moderatorCapture.occurredAtLegacyUtc());
                signature.setModeratorSignedAtUtc(moderatorCapture.occurredAtUtc());
                signature.setModeratorEvidence(moderatorCapture.evidenceJson());
                signature = campaignSignatureRepository.save(signature);
                campaign.setSignature(signature);
                campaignSignatureAuditService.recordDocumentFrozen(
                        signature,
                        SignatureActorType.MODERATOR,
                        moderator.getId(),
                        moderator.getName()
                );
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

    public CampaignResponse getCampaign(Long id) {
        return CampaignResponse.fromEntity(getCampaignOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findDetailedByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));
        return CampaignSignatureDetailsResponse.fromEntity(signature);
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

    private void validateConsentAccepted(Boolean consentAccepted) {
        if (Boolean.FALSE.equals(consentAccepted)) {
            throw new RuntimeException("Explicit electronic-signature consent is required");
        }
    }
}
