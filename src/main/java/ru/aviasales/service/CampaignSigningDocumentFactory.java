package ru.aviasales.service;

import ru.aviasales.dal.model.AdvertisingCampaign;

import java.time.LocalDate;

public final class CampaignSigningDocumentFactory {
    public static final String TEMPLATE_VERSION = "v1";

    private CampaignSigningDocumentFactory() {
    }

    public static String buildSnapshot(AdvertisingCampaign campaign) {
        return String.join("\n",
                "templateVersion=" + TEMPLATE_VERSION,
                "campaignId=" + campaign.getId(),
                "campaignName=" + safe(campaign.getName()),
                "content=" + safe(campaign.getContent()),
                "targetUrl=" + safe(campaign.getTargetUrl()),
                "dailyBudget=" + (campaign.getDailyBudget() != null ? campaign.getDailyBudget().toPlainString() : ""),
                "startDate=" + asIso(campaign.getStartDate()),
                "endDate=" + asIso(campaign.getEndDate()),
                "clientId=" + (campaign.getClient() != null ? campaign.getClient().getId() : null)
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String asIso(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
