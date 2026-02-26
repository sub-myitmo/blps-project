package ru.aviasales.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @DecimalMax(value = "1000000.00", message = "Сумма не может превышать 1,000,000")
    private BigDecimal amount;

    @Size(max = 255, message = "Описание не может превышать 255 символов")
    private String description;
}
