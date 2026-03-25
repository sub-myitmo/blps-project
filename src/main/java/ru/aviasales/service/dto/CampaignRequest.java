package ru.aviasales.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CampaignRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 100, message = "Название не может превышать 100 символов")
    private String name;

    @NotBlank(message = "Контент обязателен")
    @Size(max = 2000, message = "Контент не может превышать 2000 символов")
    private String content;

    @NotBlank(message = "URL назначения обязателен")
    @Pattern(regexp = "^(http|https)://.*$", message = "Некорректный URL")
    private String targetUrl;

    @NotNull(message = "Дневной бюджет обязателен")
    @DecimalMin(value = "0.01", message = "Дневной бюджет должен быть больше 0")
    @DecimalMax(value = "1000000.00", message = "Дневной бюджет не может превышать 1,000,000")
    private BigDecimal dailyBudget;

    @NotNull(message = "Время начала обязательно")
    private LocalDate startDate;

    private LocalDate endDate;
}
