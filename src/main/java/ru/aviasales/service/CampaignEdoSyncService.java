package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignSignatureEventType;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.SignatureActorType;
import ru.aviasales.service.edo.EdoDocumentStatus;
import ru.aviasales.service.edo.EdoOperatorClient;
import ru.aviasales.service.edo.EdoSigningStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignEdoSyncService {

    /** Minimum interval between EDO operator polls for the same document. */
    private static final Duration MIN_SYNC_INTERVAL = Duration.ofSeconds(10);

    private final EdoOperatorClient edoOperatorClient;
    private final CampaignSignatureAuditService campaignSignatureAuditService;

    @org.springframework.transaction.annotation.Transactional
    public boolean trySync(AdvertisingCampaign campaign, CampaignSignature signature) {
        if (signature == null || signature.getEdoMessageId() == null || signature.getEdoEntityId() == null) {
            return false;
        }
        if (signature.getEdoLastSyncedAtUtc() != null
                && Duration.between(signature.getEdoLastSyncedAtUtc(), Instant.now()).compareTo(MIN_SYNC_INTERVAL) < 0) {
            return false;
        }

        try {
            return sync(campaign, signature);
        } catch (RuntimeException e) {
            log.warn("EDO sync failed for campaign {}", campaign.getId(), e);
            return false;
        }
    }

    private boolean sync(AdvertisingCampaign campaign, CampaignSignature signature) {
        EdoSigningStatus status = edoOperatorClient.getSigningStatus(
                signature.getEdoMessageId(),
                signature.getEdoEntityId()
        );

        signature.setEdoDocumentStatus(status.status());
        if (status.senderCertThumbprint() != null) {
            signature.setEdoModeratorCertThumbprint(status.senderCertThumbprint());
        }
        if (status.recipientCertThumbprint() != null) {
            signature.setEdoClientCertThumbprint(status.recipientCertThumbprint());
        }
        signature.setEdoLastSyncedAtUtc(Instant.now());
        signature.setEdoRawResponse(status.rawResponse());

        CampaignSignatureAuditService.EdoEvidenceData edoEvidence =
                new CampaignSignatureAuditService.EdoEvidenceData(
                        signature.getEdoOperator(),
                        signature.getEdoMessageId(),
                        signature.getEdoEntityId(),
                        status.status(),
                        status.recipientCertThumbprint() != null
                                ? status.recipientCertThumbprint()
                                : status.senderCertThumbprint()
                );

        if (EdoDocumentStatus.isModeratorConfirmed(status.status())
                && !hasEvent(signature, CampaignSignatureEventType.MODERATOR_SIGN_CONFIRMED)) {
            Instant moderatorConfirmedAt = status.senderSignedAt() != null ? status.senderSignedAt() : Instant.now();
            CampaignSignatureAuditService.SignatureCapture moderatorCapture =
                    campaignSignatureAuditService.recordEvent(
                            signature,
                            CampaignSignatureEventType.MODERATOR_SIGN_CONFIRMED,
                            SignatureActorType.MODERATOR,
                            signature.getModeratorId(),
                            null,
                            null,
                            edoEvidence,
                            moderatorConfirmedAt
                    );
            if (status.senderSignedAt() != null) {
                signature.setModeratorSignedAt(moderatorCapture.occurredAtLegacyUtc());
                signature.setModeratorSignedAtUtc(moderatorCapture.occurredAtUtc());
            }
            signature.setModeratorEvidence(moderatorCapture.evidenceJson());
        }
        if (status.senderSignedAt() != null && signature.getModeratorSignedAtUtc() == null) {
            signature.setModeratorSignedAtUtc(status.senderSignedAt());
            signature.setModeratorSignedAt(LocalDateTime.ofInstant(status.senderSignedAt(), ZoneOffset.UTC));
        }

        if (EdoDocumentStatus.isCompleted(status.status()) && !signature.isFullySigned()) {
            Instant clientConfirmedAt = status.recipientSignedAt() != null ? status.recipientSignedAt() : Instant.now();
            CampaignSignatureAuditService.SignatureCapture clientCapture =
                    campaignSignatureAuditService.recordEvent(
                            signature,
                            CampaignSignatureEventType.CLIENT_SIGN_CONFIRMED,
                            SignatureActorType.CLIENT,
                            signature.getClientId(),
                            null,
                            null,
                            edoEvidence,
                            clientConfirmedAt
                    );
            if (status.recipientSignedAt() != null) {
                signature.setClientSignedAt(clientCapture.occurredAtLegacyUtc());
                signature.setClientSignedAtUtc(clientCapture.occurredAtUtc());
                signature.setFullySignedAtUtc(clientCapture.occurredAtUtc());
            }
            signature.setClientEvidence(clientCapture.evidenceJson());
            signature.setFullySigned(true);

            if (!hasEvent(signature, CampaignSignatureEventType.SIGNATURE_COMPLETED)) {
                campaignSignatureAuditService.recordSignatureCompleted(
                        signature,
                        SignatureActorType.SYSTEM,
                        null,
                        "EDO operator sync",
                        clientConfirmedAt
                );
            }

            if (campaign.getStatus() == CampaignStatus.AT_SIGNING) {
                campaign.transitionTo(CampaignStatus.WAITING_START);
            }
        }
        if (status.recipientSignedAt() != null && signature.getClientSignedAtUtc() == null) {
            signature.setClientSignedAtUtc(status.recipientSignedAt());
            signature.setClientSignedAt(LocalDateTime.ofInstant(status.recipientSignedAt(), ZoneOffset.UTC));
            if (signature.getFullySignedAtUtc() == null) {
                signature.setFullySignedAtUtc(status.recipientSignedAt());
            }
        }

        return true;
    }

    private boolean hasEvent(CampaignSignature signature, CampaignSignatureEventType eventType) {
        return signature.getAuditEvents().stream()
                .anyMatch(event -> event.getEventType() == eventType);
    }
}
