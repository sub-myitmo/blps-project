package ru.aviasales.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModeratorActionRequest {

    @NotNull(message = "Действие обязательно")
    private Action action;

    @Size(max = 500, message = "Комментарий не может превышать 500 символов")
    private String comment;

    private Boolean consentAccepted;

    public enum Action {
        SIGN_DOC,
        REJECT,
        PAUSE
    }
}
