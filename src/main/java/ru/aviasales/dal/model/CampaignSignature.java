package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Column(name = "moderator_signed_at")
    private LocalDateTime moderatorSignedAt;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_signed_at")
    private LocalDateTime clientSignedAt;

    @Column(name = "fully_signed", nullable = false)
    private boolean fullySigned;
}
