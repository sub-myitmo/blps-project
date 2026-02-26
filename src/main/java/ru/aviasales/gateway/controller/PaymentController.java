package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.service.dto.PaymentRequest;
import ru.aviasales.service.PaymentService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> deposit(
            @RequestHeader("Authorization") String apiKey,
            @Valid @RequestBody PaymentRequest request) {
        BigDecimal newBalance = paymentService.deposit(apiKey, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payment successful");
        response.put("newBalance", newBalance);
        response.put("amount", request.getAmount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @RequestHeader("Authorization") String apiKey) {
        BigDecimal balance = paymentService.getBalance(apiKey);

        Map<String, Object> response = new HashMap<>();
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }
}