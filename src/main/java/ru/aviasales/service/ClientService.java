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
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignSigningSagaService campaignSigningSagaService;
    private final PdfStorageService pdfStorageService;

    public ClientService(
            ClientRepository clientRepository,
            AdvertisingCampaignRepository campaignRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignSigningSagaService campaignSigningSagaService,
            PdfStorageService pdfStorageService
    ) {
        this.clientRepository = clientRepository;
        this.campaignRepository = campaignRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignSigningSagaService = campaignSigningSagaService;
        this.pdfStorageService = pdfStorageService;
    }

    @Transactional
    public List<CampaignResponse> getCampaignsByClient(Long clientId) {
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByClientIdWithDetails(clientId);
        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampaignResponse createCampaign(Long clientId, CampaignRequest request) {
        Client client = clientRepository.findActiveById(clientId)
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
    public CampaignResponse updateCampaign(Long clientId, Long campaignId, UpdateCampaignRequest request) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        validateAccessOrThrow(clientId, campaign);
        validateUpdateAllowed(campaign);
        CampaignStatus previousStatus = campaign.getStatus();

        LocalDate startDate = request.getStartDate() == null ? campaign.getStartDate() : request.getStartDate();
        LocalDate endDate = request.getEndDate() == null ? campaign.getEndDate() : request.getEndDate();
        validateUpdateDates(request.getStartDate(), startDate, endDate);

        campaign.setName(request.getName() == null ? campaign.getName() : request.getName());
        campaign.setContent(request.getContent() == null ? campaign.getContent() : request.getContent());
        campaign.setTargetUrl(request.getTargetUrl() == null ? campaign.getTargetUrl() : request.getTargetUrl());
        campaign.setDailyBudget(request.getDailyBudget() == null ? campaign.getDailyBudget() : request.getDailyBudget());
        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);

        if (previousStatus == CampaignStatus.REJECTED) {
            campaign.transitionTo(CampaignStatus.PENDING);
        } else {
            campaign.transitionTo(CampaignStatus.AT_SIGNING);
            campaignSigningSagaService.restartSigning(campaign, null);
        }

        AdvertisingCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteCampaign(Long clientId, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        validateAccessOrThrow(clientId, campaign);

        campaignRepository.delete(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long clientId, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(clientId, campaign);
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(Long clientId, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(clientId, campaign);

        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));

        return CampaignSignatureDetailsResponse.fromEntity(signature);
    }

    @Transactional
    public String getCampaignSignaturePdfUrl(Long clientId, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(clientId, campaign);

        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign signature not found"));

        signature = campaignSigningSagaService.ensurePdfStored(signature);
        return pdfStorageService.buildPresignedUrl(signature.getPdfObjectKey());
    }

    @Transactional
    public CampaignResponse actionCampaign(Long clientId, Long campaignId,
                                           ClientActionRequest request) {
        AdvertisingCampaign campaign = getCampaignForActionOrThrow(campaignId);
        Client client = validateAccessOrThrow(clientId, campaign);

        switch (request.getAction()) {
            case SIGN_DOC:
                if (campaign.getStatus() != CampaignStatus.AT_SIGNING) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Campaign must be in AT_SIGNING status for client signing");
                }
                validateConsentAccepted(request.getConsentAccepted());

                CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Campaign signature not found"));

                requireDocumentHash(signature, request.getDocumentHash());

                String actualHash = CampaignDocumentHashUtil.buildDocumentHash(campaign);
                if (!actualHash.equals(signature.getDocumentHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Campaign document hash mismatch");
                }

                signature.setClientId(client.getId());
                signature.setClientSignedAtUtc(Instant.now());
                signature.setFullySigned(true);
                signature.setFullySignedAtUtc(Instant.now());

                signature = campaignSigningSagaService.regeneratePdf(signature);
                campaign.setSignature(signature);
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
        return campaignRepository.findByIdWithDetails(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private AdvertisingCampaign getCampaignForActionOrThrow(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private Client validateAccessOrThrow(Long clientId, AdvertisingCampaign campaign) {
        Client client = getClientOrThrow(clientId);

        if (!campaign.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return client;
    }

    private Client getClientOrThrow(Long clientId) {
        return clientRepository.findActiveById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Client not found"));
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

    private void validateUpdateAllowed(AdvertisingCampaign campaign) {
        if (campaign.getStatus() != CampaignStatus.FROZEN
                && campaign.getStatus() != CampaignStatus.PAUSED_BY_CLIENT
                && campaign.getStatus() != CampaignStatus.PAUSED_BY_MODERATOR
                && campaign.getStatus() != CampaignStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Campaign can be updated only in FROZEN, PAUSED_BY_CLIENT, PAUSED_BY_MODERATOR or REJECTED status");
        }
    }

    private void validateUpdateDates(LocalDate requestedStartDate, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new RuntimeException("Start date must be not null");
        }
        if (requestedStartDate != null && requestedStartDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before end date");
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
