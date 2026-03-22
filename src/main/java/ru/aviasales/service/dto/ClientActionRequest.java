package ru.aviasales.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientActionRequest {

    @NotNull(message = "Действие обязательно")
    private ClientAction action;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean consentAccepted;
    private String documentHash;

    public enum ClientAction {
        SIGN_DOC,
        RESUME,
        PAUSE
    }
}
