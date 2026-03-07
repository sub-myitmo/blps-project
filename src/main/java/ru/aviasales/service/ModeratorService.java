package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.Comment;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.Moderator;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.ModeratorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModeratorService {

    private final ModeratorRepository moderatorRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;

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

                CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                        .orElseGet(() -> {
                            CampaignSignature newSignature = new CampaignSignature();
                            newSignature.setCampaign(campaign);
                            return newSignature;
                        });

                signature.setDocumentHash(CampaignDocumentHashUtil.buildDocumentHash(campaign));
                signature.setModeratorId(moderator.getId());
                signature.setModeratorSignedAt(LocalDateTime.now());
                signature.setClientId(null);
                signature.setClientSignedAt(null);
                signature.setFullySigned(false);

                campaign.setSignature(campaignSignatureRepository.save(signature));
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
}
