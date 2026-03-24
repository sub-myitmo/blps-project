package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignSignatureAuditEvent;
import ru.aviasales.dal.model.CampaignSignatureEventType;
import ru.aviasales.dal.model.SignatureActorType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignSignatureDetailsResponse {
    private Long campaignId;
    private String documentHash;
    private String hashAlgorithm;
    private String documentTemplateVersion;
    private String documentSnapshot;
    private Long moderatorId;
    private LocalDateTime moderatorSignedAt;
    private Instant moderatorSignedAtUtc;
    private String moderatorEvidence;
    private Long clientId;
    private LocalDateTime clientSignedAt;
    private Instant clientSignedAtUtc;
    private String clientEvidence;
    private boolean fullySigned;
    private Instant fullySignedAtUtc;
    private String edoOperator;
    private String edoMessageId;
    private String edoEntityId;
    private String edoDocumentStatus;
    private String edoModeratorCertThumbprint;
    private String edoClientCertThumbprint;
    private Instant edoLastSyncedAtUtc;
    private List<AuditEventResponse> auditEvents;

    public static CampaignSignatureDetailsResponse fromEntity(CampaignSignature signature) {
        CampaignSignatureDetailsResponse response = new CampaignSignatureDetailsResponse();
        response.setCampaignId(signature.getCampaign() != null ? signature.getCampaign().getId() : null);
        response.setDocumentHash(signature.getDocumentHash());
        response.setHashAlgorithm(signature.getHashAlgorithm());
        response.setDocumentTemplateVersion(signature.getDocumentTemplateVersion());
        response.setDocumentSnapshot(signature.getDocumentSnapshot());
        response.setModeratorId(signature.getModeratorId());
        response.setModeratorSignedAt(signature.getModeratorSignedAt());
        response.setModeratorSignedAtUtc(signature.getModeratorSignedAtUtc());
        response.setModeratorEvidence(signature.getModeratorEvidence());
        response.setClientId(signature.getClientId());
        response.setClientSignedAt(signature.getClientSignedAt());
        response.setClientSignedAtUtc(signature.getClientSignedAtUtc());
        response.setClientEvidence(signature.getClientEvidence());
        response.setFullySigned(signature.isFullySigned());
        response.setFullySignedAtUtc(signature.getFullySignedAtUtc());
        response.setEdoOperator(signature.getEdoOperator());
        response.setEdoMessageId(signature.getEdoMessageId());
        response.setEdoEntityId(signature.getEdoEntityId());
        response.setEdoDocumentStatus(signature.getEdoDocumentStatus());
        response.setEdoModeratorCertThumbprint(signature.getEdoModeratorCertThumbprint());
        response.setEdoClientCertThumbprint(signature.getEdoClientCertThumbprint());
        response.setEdoLastSyncedAtUtc(signature.getEdoLastSyncedAtUtc());
        response.setAuditEvents(signature.getAuditEvents().stream().map(AuditEventResponse::fromEntity).toList());
        return response;
    }

    @Data
    public static class AuditEventResponse {
        private Long id;
        private CampaignSignatureEventType eventType;
        private SignatureActorType actorType;
        private Long actorId;
        private String actorName;
        private String evidenceJson;
        private LocalDateTime occurredAt;
        private Instant occurredAtUtc;

        static AuditEventResponse fromEntity(CampaignSignatureAuditEvent event) {
            AuditEventResponse response = new AuditEventResponse();
            response.setId(event.getId());
            response.setEventType(event.getEventType());
            response.setActorType(event.getActorType());
            response.setActorId(event.getActorId());
            response.setActorName(event.getActorName());
            response.setEvidenceJson(event.getEvidenceJson());
            response.setOccurredAt(event.getOccurredAt());
            response.setOccurredAtUtc(event.getOccurredAtUtc());
            return response;
        }
    }
}
