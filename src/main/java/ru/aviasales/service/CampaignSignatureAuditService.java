package ru.aviasales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignSignatureAuditEvent;
import ru.aviasales.dal.model.CampaignSignatureEventType;
import ru.aviasales.dal.model.SignatureActorType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CampaignSignatureAuditService {

    private final ObjectMapper objectMapper;

    public SignatureCapture captureSignature(
            CampaignSignature signature,
            CampaignSignatureEventType eventType,
            SignatureActorType actorType,
            Long actorId,
            String actorName,
            String consentStatement,
            EdoEvidenceData edoData
    ) {
        Instant nowUtc = Instant.now();
        LocalDateTime legacyUtc = LocalDateTime.ofInstant(nowUtc, java.time.ZoneOffset.UTC);

        String evidence = buildEvidence(eventType, actorType, actorId, actorName,
                consentStatement, nowUtc, edoData);

        CampaignSignatureAuditEvent event = buildEvent(signature, eventType, actorType,
                actorId, actorName, evidence, nowUtc, legacyUtc);
        signature.getAuditEvents().add(event);

        return new SignatureCapture(nowUtc, legacyUtc, evidence);
    }

    public void recordEdoDocumentSent(CampaignSignature signature, SignatureActorType actorType,
                                       Long actorId, String actorName, EdoEvidenceData edoData) {
        Instant nowUtc = Instant.now();
        LocalDateTime legacyUtc = LocalDateTime.ofInstant(nowUtc, java.time.ZoneOffset.UTC);
        String evidence = buildEvidence(CampaignSignatureEventType.EDO_DOCUMENT_SENT,
                actorType, actorId, actorName, null, nowUtc, edoData);

        CampaignSignatureAuditEvent event = buildEvent(signature,
                CampaignSignatureEventType.EDO_DOCUMENT_SENT,
                actorType, actorId, actorName, evidence, nowUtc, legacyUtc);
        signature.getAuditEvents().add(event);
    }

    public void recordDocumentFrozen(CampaignSignature signature, SignatureActorType actorType,
                                      Long actorId, String actorName) {
        Instant nowUtc = Instant.now();
        LocalDateTime legacyUtc = LocalDateTime.ofInstant(nowUtc, java.time.ZoneOffset.UTC);
        String evidence = buildEvidence(CampaignSignatureEventType.DOCUMENT_FROZEN,
                actorType, actorId, actorName, null, nowUtc, null);

        CampaignSignatureAuditEvent event = buildEvent(signature,
                CampaignSignatureEventType.DOCUMENT_FROZEN,
                actorType, actorId, actorName, evidence, nowUtc, legacyUtc);
        signature.getAuditEvents().add(event);
    }

    public void recordSignatureCompleted(CampaignSignature signature, SignatureActorType actorType,
                                          Long actorId, String actorName) {
        Instant nowUtc = Instant.now();
        LocalDateTime legacyUtc = LocalDateTime.ofInstant(nowUtc, java.time.ZoneOffset.UTC);
        String evidence = buildEvidence(CampaignSignatureEventType.SIGNATURE_COMPLETED,
                actorType, actorId, actorName, null, nowUtc, null);

        CampaignSignatureAuditEvent event = buildEvent(signature,
                CampaignSignatureEventType.SIGNATURE_COMPLETED,
                actorType, actorId, actorName, evidence, nowUtc, legacyUtc);
        signature.getAuditEvents().add(event);
    }

    private CampaignSignatureAuditEvent buildEvent(
            CampaignSignature signature,
            CampaignSignatureEventType eventType,
            SignatureActorType actorType,
            Long actorId,
            String actorName,
            String evidence,
            Instant occurredAtUtc,
            LocalDateTime occurredAtLegacyUtc
    ) {
        CampaignSignatureAuditEvent event = new CampaignSignatureAuditEvent();
        event.setSignature(signature);
        event.setEventType(eventType);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setActorName(actorName);
        event.setEvidenceJson(evidence);
        event.setOccurredAtUtc(occurredAtUtc);
        event.setOccurredAt(occurredAtLegacyUtc);
        return event;
    }

    private String buildEvidence(
            CampaignSignatureEventType eventType,
            SignatureActorType actorType,
            Long actorId,
            String actorName,
            String consentStatement,
            Instant occurredAtUtc,
            EdoEvidenceData edoData
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType.name());
        payload.put("actorType", actorType.name());
        payload.put("actorId", actorId);
        payload.put("actorName", actorName);
        if (consentStatement != null) {
            payload.put("consentStatement", consentStatement);
        }
        payload.put("occurredAtUtc", occurredAtUtc.toString());
        if (edoData != null) {
            payload.put("edoOperator", edoData.operator());
            payload.put("edoMessageId", edoData.messageId());
            payload.put("edoEntityId", edoData.entityId());
            payload.put("edoStatus", edoData.status());
            if (edoData.certThumbprint() != null) {
                payload.put("edoCertThumbprint", edoData.certThumbprint());
            }
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize signature evidence", e);
        }
    }

    public record EdoEvidenceData(
            String operator,
            String messageId,
            String entityId,
            String status,
            String certThumbprint
    ) {}

    public record SignatureCapture(Instant occurredAtUtc, LocalDateTime occurredAtLegacyUtc, String evidenceJson) {}
}
