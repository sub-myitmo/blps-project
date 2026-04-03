package ru.aviasales.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateCampaignRequest {

    @Size(max = 100, message = "Название не может превышать 100 символов")
    private String name;

    @Size(max = 2000, message = "Контент не может превышать 2000 символов")
    private String content;

    @Pattern(regexp = "^(http|https)://.*$", message = "Некорректный URL")
    private String targetUrl;

    @DecimalMin(value = "0.01", message = "Дневной бюджет должен быть больше 0")
    @DecimalMax(value = "1000000.00", message = "Дневной бюджет не может превышать 1,000,000")
    private BigDecimal dailyBudget;

    private LocalDate startDate;

    private LocalDate endDate;
}
