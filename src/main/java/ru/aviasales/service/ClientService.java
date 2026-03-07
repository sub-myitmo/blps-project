package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.*;
import ru.aviasales.service.dto.CampaignRequest;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.service.dto.ClientActionRequest;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;

    @Transactional
    public CampaignResponse createCampaign(String apiKey, CampaignRequest request) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        validateDates(request.getStartDate(), request.getEndDate());

        AdvertisingCampaign campaign = new AdvertisingCampaign();
        campaign.setName(request.getName());
        campaign.setContent(request.getContent());
        campaign.setTargetUrl(request.getTargetUrl());
        campaign.setDailyBudget(request.getDailyBudget());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setClient(client);
        campaign.setStatus(CampaignStatus.PENDING);

        AdvertisingCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public CampaignResponse getCampaign(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);

        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional
    public CampaignResponse actionCampaign(String apiKey, Long campaignId,
                                           ClientActionRequest request) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        Client client = validateAccessOrThrow(apiKey, campaign);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.AT_SIGNING) {
                    throw new RuntimeException("Campaign must be in AT_SIGNING status for client signing");
                }

                CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                        .orElseThrow(() -> new RuntimeException("Campaign is not signed by moderator"));

                if (signature.getModeratorId() == null || signature.getModeratorSignedAt() == null) {
                    throw new RuntimeException("Campaign is not signed by moderator");
                }

                String actualHash = CampaignDocumentHashUtil.buildDocumentHash(campaign);
                if (!actualHash.equals(signature.getDocumentHash())) {
                    throw new RuntimeException("Campaign document hash mismatch");
                }

                signature.setClientId(client.getId());
                signature.setClientSignedAt(LocalDateTime.now());
                signature.setFullySigned(true);

                campaign.setSignature(campaignSignatureRepository.save(signature));
                campaign.transitionTo(CampaignStatus.WAITING_START);
                break;
            case RESUME:
                validateDates(request.getStartDate(), request.getEndDate());
                campaign.transitionTo(CampaignStatus.WAITING_START);
                campaign.setStartDate(request.getStartDate());
                campaign.setEndDate(request.getEndDate());
                break;
            case PAUSE:
                campaign.transitionTo(CampaignStatus.PAUSED_BY_CLIENT);
                break;
            default:
                throw new RuntimeException("Invalid action for client");
        }

        return CampaignResponse.fromEntity(campaignRepository.save(campaign));
    }

    private AdvertisingCampaign getCampaignOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    private Client validateAccessOrThrow(String apiKey, AdvertisingCampaign campaign) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!campaign.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Access denied");
        }

        return client;
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new RuntimeException("Start date must be not null");
        }
        if (startDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }
        if (endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new RuntimeException("Start date must be before end date");
            }
        }
    }
}
