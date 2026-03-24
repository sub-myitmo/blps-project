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

    public SignatureCapture recordEvent(
            CampaignSignature signature,
            CampaignSignatureEventType eventType,
            SignatureActorType actorType,
            Long actorId,
            String actorName,
            String consentStatement,
            EdoEvidenceData edoData
    ) {
        return recordEvent(signature, eventType, actorType, actorId, actorName,
                consentStatement, edoData, Instant.now());
    }

    public SignatureCapture recordEvent(
            CampaignSignature signature,
            CampaignSignatureEventType eventType,
            SignatureActorType actorType,
            Long actorId,
            String actorName,
            String consentStatement,
            EdoEvidenceData edoData,
            Instant occurredAtUtc
    ) {
        LocalDateTime legacyUtc = LocalDateTime.ofInstant(occurredAtUtc, java.time.ZoneOffset.UTC);

        String evidence = buildEvidence(eventType, actorType, actorId, actorName,
                consentStatement, occurredAtUtc, edoData);

        CampaignSignatureAuditEvent event = buildEvent(signature, eventType, actorType,
                actorId, actorName, evidence, occurredAtUtc, legacyUtc);
        signature.getAuditEvents().add(event);

        return new SignatureCapture(occurredAtUtc, legacyUtc, evidence);
    }

    public void recordEdoDocumentSent(CampaignSignature signature, SignatureActorType actorType,
                                       Long actorId, String actorName, String consentStatement,
                                       EdoEvidenceData edoData) {
        recordEvent(signature, CampaignSignatureEventType.EDO_DOCUMENT_SENT,
                actorType, actorId, actorName, consentStatement, edoData);
    }

    public void recordEdoCounterSignInitiated(CampaignSignature signature, SignatureActorType actorType,
                                              Long actorId, String actorName, String consentStatement,
                                              EdoEvidenceData edoData) {
        recordEvent(signature, CampaignSignatureEventType.EDO_COUNTERSIGN_INITIATED,
                actorType, actorId, actorName, consentStatement, edoData);
    }

    public void recordDocumentFrozen(CampaignSignature signature, SignatureActorType actorType,
                                      Long actorId, String actorName) {
        recordEvent(signature, CampaignSignatureEventType.DOCUMENT_FROZEN,
                actorType, actorId, actorName, null, null);
    }

    public void recordSignatureCompleted(CampaignSignature signature, SignatureActorType actorType,
                                          Long actorId, String actorName) {
        recordSignatureCompleted(signature, actorType, actorId, actorName, Instant.now());
    }

    public void recordSignatureCompleted(CampaignSignature signature, SignatureActorType actorType,
                                         Long actorId, String actorName, Instant occurredAtUtc) {
        recordEvent(signature, CampaignSignatureEventType.SIGNATURE_COMPLETED,
                actorType, actorId, actorName, null, null, occurredAtUtc);
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
