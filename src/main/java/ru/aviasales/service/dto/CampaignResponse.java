package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.AdvertisingCampaign;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignResponse {
    private Long id;
    private String name;
    private String content;
    private String targetUrl;
    private BigDecimal dailyBudget;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private AdvertisingCampaign.CampaignStatus status;
    private LocalDateTime createdAt;
    private List<CommentResponse> moderationComments;

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
        return response;
    }
}
