package ru.aviasales.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClientActionRequest {

    @NotNull(message = "Действие обязательно")
    private ClientAction action;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean consentAccepted;
    private String documentHash;

    public enum ClientAction {
        SIGN_DOC,
        RESUME,
        PAUSE
    }
}
