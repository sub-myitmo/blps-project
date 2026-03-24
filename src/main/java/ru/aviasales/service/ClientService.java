package ru.aviasales.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.*;
import ru.aviasales.service.dto.CampaignRequest;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.service.dto.CampaignSignatureDetailsResponse;
import ru.aviasales.service.dto.ClientActionRequest;
import ru.aviasales.service.edo.EdoCounterSignResult;
import ru.aviasales.service.edo.EdoOperatorClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignSignatureAuditService campaignSignatureAuditService;
    private final EdoOperatorClient edoOperatorClient;
    private final String edoClientBoxId;

    public ClientService(
            ClientRepository clientRepository,
            AdvertisingCampaignRepository campaignRepository,
            CampaignSignatureRepository campaignSignatureRepository,
            CampaignSignatureAuditService campaignSignatureAuditService,
            EdoOperatorClient edoOperatorClient,
            @Value("${edo.client-box-id:stub-client-box}") String edoClientBoxId
    ) {
        this.clientRepository = clientRepository;
        this.campaignRepository = campaignRepository;
        this.campaignSignatureRepository = campaignSignatureRepository;
        this.campaignSignatureAuditService = campaignSignatureAuditService;
        this.edoOperatorClient = edoOperatorClient;
        this.edoClientBoxId = edoClientBoxId;
    }

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

    @Transactional(readOnly = true)
    public CampaignSignatureDetailsResponse getCampaignSignature(String apiKey, Long campaignId) {
        AdvertisingCampaign campaign = getCampaignOrThrow(campaignId);
        validateAccessOrThrow(apiKey, campaign);

        CampaignSignature signature = campaignSignatureRepository.findDetailedByCampaign(campaign)
                .orElseThrow(() -> new RuntimeException("Campaign signature not found"));

        return CampaignSignatureDetailsResponse.fromEntity(signature);
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
                validateConsentAccepted(request.getConsentAccepted());

                CampaignSignature signature = campaignSignatureRepository.findByCampaign(campaign)
                        .orElseThrow(() -> new RuntimeException("Campaign is not signed by moderator"));

                if (signature.getModeratorId() == null || signature.getModeratorSignedAt() == null) {
                    throw new RuntimeException("Campaign is not signed by moderator");
                }

                requireDocumentHash(signature, request.getDocumentHash());

                // Verify document integrity — catch illegal mutations after freeze
                String actualHash = CampaignDocumentHashUtil.buildDocumentHash(campaign);
                if (!actualHash.equals(signature.getDocumentHash())) {
                    throw new RuntimeException("Campaign document hash mismatch");
                }

                // Verify operator document exists
                if (signature.getEdoMessageId() == null || signature.getEdoEntityId() == null) {
                    throw new RuntimeException("No ЭДО operator document found for this signature");
                }

                // Initiate countersign via ЭДО operator
                EdoCounterSignResult counterSignResult = edoOperatorClient.initiateCounterSign(
                        signature.getEdoMessageId(),
                        signature.getEdoEntityId(),
                        edoClientBoxId
                );

                // Update operator fields
                signature.setEdoDocumentStatus(counterSignResult.status());
                signature.setEdoClientBoxId(edoClientBoxId);
                signature.setEdoClientCertThumbprint(counterSignResult.recipientCertThumbprint());
                signature.setEdoLastSyncedAtUtc(Instant.now());
                signature.setEdoRawResponse(counterSignResult.rawResponse());

                // Audit: client signed via operator
                CampaignSignatureAuditService.EdoEvidenceData edoEvidence =
                        new CampaignSignatureAuditService.EdoEvidenceData(
                                signature.getEdoOperator(),
                                signature.getEdoMessageId(),
                                signature.getEdoEntityId(),
                                counterSignResult.status(),
                                counterSignResult.recipientCertThumbprint()
                        );

                signature.setClientId(client.getId());
                CampaignSignatureAuditService.SignatureCapture clientCapture =
                        campaignSignatureAuditService.captureSignature(
                                signature,
                                CampaignSignatureEventType.CLIENT_SIGNED,
                                SignatureActorType.CLIENT,
                                client.getId(),
                                client.getName(),
                                CampaignSignaturePolicy.CLIENT_CONSENT_STATEMENT,
                                edoEvidence
                        );
                signature.setClientSignedAt(clientCapture.occurredAtLegacyUtc());
                signature.setClientSignedAtUtc(clientCapture.occurredAtUtc());
                signature.setClientEvidence(clientCapture.evidenceJson());
                signature.setFullySigned(true);
                signature.setFullySignedAtUtc(clientCapture.occurredAtUtc());

                campaign.setSignature(campaignSignatureRepository.save(signature));
                campaignSignatureAuditService.recordSignatureCompleted(
                        signature,
                        SignatureActorType.CLIENT,
                        client.getId(),
                        client.getName()
                );
                campaign.transitionTo(CampaignStatus.WAITING_START);
                break;
            case RESUME:
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

    private void validateConsentAccepted(Boolean consentAccepted) {
        if (Boolean.FALSE.equals(consentAccepted)) {
            throw new RuntimeException("Explicit electronic-signature consent is required");
        }
    }

    private void requireDocumentHash(CampaignSignature signature, String documentHash) {
        if (documentHash != null && !documentHash.isBlank() && !signature.getDocumentHash().equals(documentHash)) {
            throw new RuntimeException("Document hash confirmation mismatch");
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
            throw new RuntimeException("Signed campaign terms cannot be changed without a new signing cycle");
        }
    }
}
