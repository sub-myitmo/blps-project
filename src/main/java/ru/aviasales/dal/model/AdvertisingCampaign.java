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

    public enum CampaignStatus {
        PENDING,        // На модерации
        APPROVED,       // Одобрено, но не активно (ждет старта или нет денег)
        ACTIVE,         // Активно (деньги списываются)
        PAUSED_BY_CLIENT,    // Приостановлено клиентом
        PAUSED_BY_MODERATOR, // Приостановлено модератором
        FROZEN,         // Заморожено (нет денег)
        REJECTED,       // Отклонено модератором
        COMPLETED,       // Завершено (по дате окончания)
        AT_SIGNING_BY_CLIENT,      // На подписании клиентом
        AT_SIGNING_BY_MODERATOR      // На подписании модератором
        // COMPLETED, PAUSED_BY_CLIENT, PAUSED_BY_MODERATOR, FROZEN - одно и то же по факту
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
