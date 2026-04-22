package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aviasales.service.AdminService;
import ru.aviasales.service.ModeratorService;
import ru.aviasales.service.dto.AccountResponse;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ManagerRequest;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ModeratorService moderatorService;

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(moderatorService.getAllCampaigns());
    }

    @PostMapping("/managers")
    public ResponseEntity<AccountResponse> createManager(@Valid @RequestBody ManagerRequest request) {
        return ResponseEntity.ok(adminService.createManager(request));
    }

    @DeleteMapping("/managers/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        adminService.deleteManager(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        adminService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
