package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.Comment;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.Moderator;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.ModeratorRepository;

@Service
@RequiredArgsConstructor
public class ModeratorService {

    private final ModeratorRepository moderatorRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CampaignResponse actionCampaign(String apiKey, Long campaignId,
                                             ModeratorActionRequest request) {
        Moderator moderator = moderatorRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if ((campaign.getStatus() == AdvertisingCampaign.CampaignStatus.ACTIVE
                && request.getAction() == ModeratorActionRequest.Action.PAUSE)
                || campaign.getStatus() != AdvertisingCampaign.CampaignStatus.PENDING) {
            throw new RuntimeException("Campaign is not pending moderation");
        }

        Comment comment = new Comment();
        comment.setModerator(moderator);
        comment.setModerationComment(request.getComment());
        comment.setCampaign(campaign);
        commentRepository.save(comment);

        switch (request.getAction()) {
            case APPROVE:
                campaign.setStatus(AdvertisingCampaign.CampaignStatus.APPROVED);
                break;
            case REJECT:
                campaign.setStatus(AdvertisingCampaign.CampaignStatus.REJECTED);
                break;
            case PAUSE:
                campaign.setStatus(AdvertisingCampaign.CampaignStatus.PAUSED_BY_MODERATOR);
            default:
                throw new RuntimeException("Invalid action for moderation");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse signatureCampaign(String apiKey, Long campaignId) {
        Moderator moderator = moderatorRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() == AdvertisingCampaign.CampaignStatus.APPROVED) {
            campaign.setStatus(AdvertisingCampaign.CampaignStatus.AT_SIGNING_BY_MODERATOR);
        } else {
            throw new RuntimeException("Кампания должна быть в статусе APPROVED");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }
}
