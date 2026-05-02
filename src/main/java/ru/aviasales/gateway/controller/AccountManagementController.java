package ru.aviasales.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aviasales.service.AdminService;

@RestController
@RequestMapping({"/api/manager", "/api/moderator"})
@RequiredArgsConstructor
public class AccountManagementController {

    private final AdminService adminService;

    @DeleteMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE_ANY')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        adminService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
