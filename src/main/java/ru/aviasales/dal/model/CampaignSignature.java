package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaign_signatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private AdvertisingCampaign campaign;

    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;

    @Column(name = "hash_algorithm", nullable = false, length = 32)
    private String hashAlgorithm;

    @Column(name = "signature_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private SignatureType signatureType;

    @Column(name = "document_template_version", nullable = false, length = 64)
    private String documentTemplateVersion;

    @Lob
    @Column(name = "document_snapshot", nullable = false, columnDefinition = "TEXT")
    private String documentSnapshot;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Column(name = "moderator_signed_at")
    private LocalDateTime moderatorSignedAt;

    @Column(name = "moderator_signed_at_utc")
    private Instant moderatorSignedAtUtc;

    @Lob
    @Column(name = "moderator_evidence", columnDefinition = "TEXT")
    private String moderatorEvidence;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_signed_at")
    private LocalDateTime clientSignedAt;

    @Column(name = "client_signed_at_utc")
    private Instant clientSignedAtUtc;

    @Lob
    @Column(name = "client_evidence", columnDefinition = "TEXT")
    private String clientEvidence;

    @Column(name = "fully_signed", nullable = false)
    private boolean fullySigned;

    @Column(name = "fully_signed_at_utc")
    private Instant fullySignedAtUtc;

    @OneToMany(mappedBy = "signature", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAtUtc ASC")
    private List<CampaignSignatureAuditEvent> auditEvents = new ArrayList<>();
}
