package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.aviasales.security.UserPrincipal;
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
    @PreAuthorize("hasAuthority('PAYMENT_TOPUP')")
    public ResponseEntity<Map<String, Object>> deposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentRequest request) {
        BigDecimal newBalance = paymentService.deposit(principal.getClientId(), request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payment successful");
        response.put("newBalance", newBalance);
        response.put("amount", request.getAmount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW_OWN')")
    public ResponseEntity<Map<String, Object>> getBalance(
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal balance = paymentService.getBalance(principal.getClientId());

        Map<String, Object> response = new HashMap<>();
        response.put("balance", balance);

        return ResponseEntity.ok(response);
    }
}
