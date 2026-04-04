package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @Column(name = "document_template_version", nullable = false, length = 64)
    private String documentTemplateVersion;

    @Lob
    @Column(name = "document_snapshot", nullable = false, columnDefinition = "TEXT")
    private String documentSnapshot;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Column(name = "moderator_signed_at_utc")
    private Instant moderatorSignedAtUtc;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_signed_at_utc")
    private Instant clientSignedAtUtc;

    @Column(name = "fully_signed", nullable = false)
    private boolean fullySigned;

    @Column(name = "fully_signed_at_utc")
    private Instant fullySignedAtUtc;
}
