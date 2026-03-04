package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "advertising_campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyBudget;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void transitionTo(CampaignStatus newStatus) {
        if (status.canTransitionTo(newStatus)) {
            this.status = newStatus;
        } else {
            throw new RuntimeException("Действие недоступно");
        }

    }
}
