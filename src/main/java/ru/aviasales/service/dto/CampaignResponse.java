package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.CampaignSignature;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignResponse {
    private Long id;
    private String name;
    private String content;
    private String targetUrl;
    private BigDecimal dailyBudget;
    private LocalDate startDate;
    private LocalDate endDate;
    private CampaignStatus status;
    private LocalDateTime createdAt;
    private List<CommentResponse> moderationComments;
    private String documentHash;
    private Instant moderatorSignedAtUtc;
    private Instant clientSignedAtUtc;
    private boolean fullySigned;

    public static CampaignResponse fromEntity(AdvertisingCampaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setName(campaign.getName());
        response.setContent(campaign.getContent());
        response.setTargetUrl(campaign.getTargetUrl());
        response.setDailyBudget(campaign.getDailyBudget());
        response.setStartDate(campaign.getStartDate());
        response.setEndDate(campaign.getEndDate());
        response.setStatus(campaign.getStatus());
        response.setCreatedAt(campaign.getCreatedAt());
        response.setModerationComments(campaign.getComments().stream().map(CommentResponse::fromEntity).toList());

        CampaignSignature signature = campaign.getSignature();
        if (signature != null) {
            response.setDocumentHash(signature.getDocumentHash());
            response.setModeratorSignedAtUtc(signature.getModeratorSignedAtUtc());
            response.setClientSignedAtUtc(signature.getClientSignedAtUtc());
            response.setFullySigned(signature.isFullySigned());
        }

        return response;
    }
}
