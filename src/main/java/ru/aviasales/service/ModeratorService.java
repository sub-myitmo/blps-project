package ru.aviasales.service;

import org.springframework.beans.factory.annotation.Value;
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
import ru.aviasales.service.edo.EdoDocumentPayload;
import ru.aviasales.service.edo.EdoOperatorClient;
import ru.aviasales.service.edo.EdoSendResult;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModeratorService {

    private final ModeratorRepository moderatorRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignSignatureAuditService campaignSignatureAuditService;
    private final CampaignEdoSyncService campaignEdoSyncService;
    private final EdoOperatorClient edoOperatorClient;
    private final CampaignDocumentPdfService campaignDocumentPdfService;
    private final String edoModeratorBoxId;
    private final String edoClientBoxId;

    public ModeratorService(
            ModeratorRepository moderatorRepository,
            AdvertisingCampaignRepository campaignRepository,
            CommentRepository commentRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignSignatureAuditService campaignSignatureAuditService,
            CampaignEdoSyncService campaignEdoSyncService,
            EdoOperatorClient edoOperatorClient,
            CampaignDocumentPdfService campaignDocumentPdfService,
            @Value("${edo.moderator-box-id:stub-moderator-box}") String edoModeratorBoxId,
            @Value("${edo.client-box-id:stub-client-box}") String edoClientBoxId
    ) {
        this.moderatorRepository = moderatorRepository;
        this.campaignRepository = campaignRepository;
        this.commentRepository = commentRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignSignatureAuditService = campaignSignatureAuditService;
        this.campaignEdoSyncService = campaignEdoSyncService;
        this.edoOperatorClient = edoOperatorClient;
        this.campaignDocumentPdfService = campaignDocumentPdfService;
        this.edoModeratorBoxId = edoModeratorBoxId;
        this.edoClientBoxId = edoClientBoxId;
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
                signature.setSignatureType(null); // no longer self-assigned
                signature.setDocumentTemplateVersion(CampaignSigningDocumentFactory.TEMPLATE_VERSION);
                signature.setDocumentSnapshot(documentSnapshot);

                // Reset signing state for fresh cycle
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

                // Reset operator fields
                signature.setEdoOperator(null);
                signature.setEdoMessageId(null);
                signature.setEdoEntityId(null);
                signature.setEdoDocumentStatus(null);
                signature.setEdoModeratorBoxId(null);
                signature.setEdoClientBoxId(null);
                signature.setEdoModeratorCertThumbprint(null);
                signature.setEdoClientCertThumbprint(null);
                signature.setEdoLastSyncedAtUtc(null);
                signature.setEdoRawResponse(null);

                signature = campaignSignatureRepository.save(signature);

                // Send document to ЭДО operator for moderator-side signing
                EdoSendResult edoResult = edoOperatorClient.sendDocumentForSigning(
                        new EdoDocumentPayload(
                                documentSnapshot,
                                documentHash,
                                edoModeratorBoxId,
                                edoClientBoxId,
                                "Campaign #" + campaign.getId() + " advertising agreement"
                        )
                );

                // Store operator identifiers
                signature.setEdoOperator(edoResult.operator());
                signature.setEdoMessageId(edoResult.messageId());
                signature.setEdoEntityId(edoResult.entityId());
                signature.setEdoDocumentStatus(edoResult.status());
                signature.setEdoModeratorBoxId(edoModeratorBoxId);
                signature.setEdoClientBoxId(edoClientBoxId);
                signature.setEdoModeratorCertThumbprint(edoResult.senderCertThumbprint());
                signature.setEdoLastSyncedAtUtc(Instant.now());
                signature.setEdoRawResponse(edoResult.rawResponse());

                CampaignSignatureAuditService.EdoEvidenceData edoEvidence =
                        new CampaignSignatureAuditService.EdoEvidenceData(
                                edoResult.operator(),
                                edoResult.messageId(),
                                edoResult.entityId(),
                                edoResult.status(),
                                edoResult.senderCertThumbprint()
                        );

                signature = campaignSignatureRepository.save(signature);
                campaign.setSignature(signature);

                campaignSignatureAuditService.recordDocumentFrozen(
                        signature,
                        SignatureActorType.MODERATOR,
                        moderator.getId(),
                        moderator.getName()
                );
                campaignSignatureAuditService.recordEdoDocumentSent(
                        signature,
                        SignatureActorType.MODERATOR,
                        moderator.getId(),
                        moderator.getName(),
                        CampaignSignaturePolicy.MODERATOR_CONSENT_STATEMENT,
                        edoEvidence
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

    @Transactional
    public CampaignResponse getCampaign(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        campaignEdoSyncService.trySync(campaign, campaign.getSignature());
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional
    public CampaignSignatureDetailsResponse getCampaignSignature(Long id) {
        AdvertisingCampaign campaign = getCampaignOrThrow(id);
        CampaignSignature signature = campaignSignatureRepository.findDetailedByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));
        campaignEdoSyncService.trySync(campaign, signature);
        return CampaignSignatureDetailsResponse.fromEntity(signature);
    }

    public List<CampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByStatus(status);
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] getFrozenDocumentPdf(Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);

        CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));

        return campaignDocumentPdfService.generateFrozenDocumentPdf(signature);
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
