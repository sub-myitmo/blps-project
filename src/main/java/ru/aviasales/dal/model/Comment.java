package ru.aviasales.dal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private AdvertisingCampaign campaign;

    @Column(name = "moderation_comment", nullable = false)
    private String moderationComment;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
