package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.service.dto.CampaignRequest;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.ClientRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final AdvertisingCampaignRepository campaignRepository;

    @Transactional
    public CampaignResponse createCampaign(String apiKey, CampaignRequest request) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Валидация дат
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Start date must be before end date");
            }
            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Start date cannot be in the past");
            }
        }

        AdvertisingCampaign campaign = new AdvertisingCampaign();
        campaign.setName(request.getName());
        campaign.setContent(request.getContent());
        campaign.setTargetUrl(request.getTargetUrl());
        campaign.setDailyBudget(request.getDailyBudget());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setClient(client);
        campaign.setStatus(AdvertisingCampaign.CampaignStatus.PENDING);

        AdvertisingCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public CampaignResponse pauseCampaign(String apiKey, Long campaignId) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (campaign.getStatus() == AdvertisingCampaign.CampaignStatus.ACTIVE) {
            campaign.setStatus(AdvertisingCampaign.CampaignStatus.PAUSED_BY_CLIENT);
        } else {
            throw new RuntimeException("Campaign is not active");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse activeCampaign(String apiKey, Long campaignId) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (hasEnoughFunds(client, campaign.getDailyBudget())) {
            campaign.setStatus(AdvertisingCampaign.CampaignStatus.ACTIVE);
        } else {
            campaign.setStatus(AdvertisingCampaign.CampaignStatus.FROZEN);
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse signatureCampaign(String apiKey, Long campaignId) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() == AdvertisingCampaign.CampaignStatus.AT_SIGNING_BY_MODERATOR) {
            campaign.setStatus(AdvertisingCampaign.CampaignStatus.AT_SIGNING_BY_CLIENT);
        } else {
            throw new RuntimeException("Действие недоступно");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    private boolean hasEnoughFunds(Client client, BigDecimal dailyBudget) {
        return client.getBalance().compareTo(dailyBudget) >= 0;
    }
}
