package ru.aviasales.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.*;
import ru.aviasales.service.doc.*;
import ru.aviasales.service.dto.*;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.ClientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private static final Set<CampaignStatus> CANCELABLE_STATUSES = Set.of(
            CampaignStatus.PENDING,
            CampaignStatus.REJECTED,
            CampaignStatus.WAITING_START,
            CampaignStatus.PAUSED_BY_CLIENT,
            CampaignStatus.PAUSED_BY_MODERATOR,
            CampaignStatus.FROZEN,
            CampaignStatus.AT_SIGNING
    );

    private final ClientRepository clientRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignDocumentPdfService campaignDocumentPdfService;

    public ClientService(
            ClientRepository clientRepository,
            AdvertisingCampaignRepository campaignRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignDocumentPdfService campaignDocumentPdfService
    ) {
        this.clientRepository = clientRepository;
        this.campaignRepository = campaignRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignDocumentPdfService = campaignDocumentPdfService;
    }

    @Transactional
    public List<CampaignResponse> getCampaignsByClient(String apiKey) {
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByClient(getClientOrThrow(apiKey));
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampaignResponse createCampaign(String apiKey, CampaignRequest request) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Client not found"));
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
    public CampaignResponse updateCampaign(String apiKey, Long campaignId, UpdateCampaignRequest request) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);
        LocalDate startDate = request.getStartDate() == null ? campaign.getStartDate() : request.getStartDate();
        LocalDate endDate = request.getEndDate() == null ? campaign.getEndDate() : request.getEndDate();

        validateDates(startDate, endDate);

        campaign.setName(request.getName() == null ? campaign.getName() : request.getName());
        campaign.setContent(request.getContent() == null ? campaign.getContent() : request.getContent());
        campaign.setTargetUrl(request.getTargetUrl() == null ? campaign.getTargetUrl() : request.getTargetUrl());
        campaign.setDailyBudget(request.getDailyBudget() == null ? campaign.getDailyBudget() : request.getDailyBudget());
        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);
        campaign.transitionTo(CampaignStatus.PENDING);

        AdvertisingCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteCampaign(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);

        if (!CANCELABLE_STATUSES.contains(campaign.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete campaign in status " + campaign.getStatus() + ". Pause it first.");
        }

        campaignRepository.delete(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);

        CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));

        return CampaignSignatureDetailsResponse.fromEntity(signature);
    }

    @Transactional(readOnly = true)
    public byte[] getCampaignSignaturePdf(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);

        CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));

        return campaignDocumentPdfService.generatePdf(signature);
    }

    @Transactional
    public CampaignResponse actionCampaign(String apiKey, Long campaignId,
                                           ClientActionRequest request) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        Client client = validateAccessOrThrow(apiKey, campaign);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.AT_SIGNING) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Campaign must be in AT_SIGNING status for client signing");
                }
                validateConsentAccepted(request.getConsentAccepted());

                CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Campaign is not signed by moderator"));

                requireDocumentHash(signature, request.getDocumentHash());

                // Verify document integrity — catch illegal mutations after freeze
                String actualHash = CampaignDocumentHashUtil.buildDocumentHash(campaign);
                if (!actualHash.equals(signature.getDocumentHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Campaign document hash mismatch");
                }

                // Record client signature
                signature.setClientId(client.getId());
                signature.setClientSignedAtUtc(Instant.now());
                signature.setFullySigned(true);
                signature.setFullySignedAtUtc(Instant.now());

                campaign.setSignature(campaignSignatureRepository.save(signature));
                campaign.transitionTo(CampaignStatus.WAITING_START);
                break;
            case RESUME:
                validateResumeAllowed(campaign);
                rejectSignedTermChanges(campaign, request);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private Client getClientOrThrow(String apiKey) {
        return clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Client not found"));
    }

    private AdvertisingCampaign getCampaignForActionOrThrow(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private Client validateAccessOrThrow(String apiKey, AdvertisingCampaign campaign) {
        Client client = getClientOrThrow(apiKey);

        if (!campaign.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return client;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new RuntimeException("Start date must be not null");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }
        if (endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new RuntimeException("Start date must be before end date");
            }
        }
    }

    private void validateConsentAccepted(Boolean consentAccepted) {
        if (Boolean.FALSE.equals(consentAccepted)) {
            throw new RuntimeException("Explicit electronic-signature consent is required");
        }
    }

    private void validateResumeAllowed(AdvertisingCampaign campaign) {
        if (campaign.getStatus() != CampaignStatus.PAUSED_BY_CLIENT
                && campaign.getStatus() != CampaignStatus.PAUSED_BY_MODERATOR
                && campaign.getStatus() != CampaignStatus.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Resume is allowed only for paused or frozen campaigns");
        }
    }

    private void requireDocumentHash(CampaignSignature signature, String documentHash) {
        if (documentHash != null && !documentHash.isBlank() && !signature.getDocumentHash().equals(documentHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document hash confirmation mismatch");
        }
    }

    private void rejectSignedTermChanges(AdvertisingCampaign campaign, ClientActionRequest request) {
        CampaignSignature signature = campaign.getSignature();
        if (signature == null || !signature.isFullySigned()) {
            return;
        }

        boolean startDateChanged = !Objects.equals(campaign.getStartDate(), request.getStartDate());
        boolean endDateChanged = !Objects.equals(campaign.getEndDate(), request.getEndDate());
        if (startDateChanged || endDateChanged) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Signed campaign terms cannot be changed without a new signing cycle");
        }
    }
}
