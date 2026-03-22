package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_signature_audit_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSignatureAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signature_id", nullable = false)
    private CampaignSignature signature;

    @Column(name = "event_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private CampaignSignatureEventType eventType;

    @Column(name = "actor_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private SignatureActorType actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Lob
    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "occurred_at_utc", nullable = false)
    private Instant occurredAtUtc;
}