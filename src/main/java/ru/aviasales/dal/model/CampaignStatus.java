package ru.aviasales.dal.model;

import java.util.List;

public enum CampaignStatus {
    PENDING,        // На модерации
    WAITING_START,       // Ожидает старта
    ACTIVE,         // Активно
    PAUSED_BY_CLIENT,    // Приостановлено клиентом
    PAUSED_BY_MODERATOR, // Приостановлено модератором
    FROZEN,         // Заморожено (нет денег)
    REJECTED,       // Отклонено
    COMPLETED,      // Завершено (время окончилось)
    AT_SIGNING;      // На подписании у клиента

    public boolean canTransitionTo(CampaignStatus newStatus) {
        return getAllowedTransitions().contains(newStatus);
    }

    public List<CampaignStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING -> List.of(
                    REJECTED,
                    AT_SIGNING
            );
            case ACTIVE -> List.of(
                    PAUSED_BY_CLIENT,
                    PAUSED_BY_MODERATOR,
                    COMPLETED,
                    FROZEN
            );
            case PAUSED_BY_CLIENT, PAUSED_BY_MODERATOR, FROZEN, AT_SIGNING -> List.of(
                    WAITING_START
            );
            case COMPLETED -> List.of();
            case WAITING_START -> List.of(
                    ACTIVE,
                    FROZEN
            );
            case REJECTED -> List.of(
                    PENDING
            );
        };
    }
}
