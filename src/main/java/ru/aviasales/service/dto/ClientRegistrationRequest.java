package ru.aviasales.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRegistrationRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(max = 255, message = "Логин не может превышать 255 символов")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 255, message = "Пароль должен содержать от 6 до 255 символов")
    private String password;

    @NotBlank(message = "Имя клиента обязательно")
    @Size(max = 255, message = "Имя клиента не может превышать 255 символов")
    private String name;
}
