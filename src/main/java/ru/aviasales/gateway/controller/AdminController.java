package ru.aviasales.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.service.AdminService;
import ru.aviasales.service.ModeratorService;
import ru.aviasales.service.PrivilegeService;
import ru.aviasales.service.dto.AccountResponse;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ManagerRequest;
import ru.aviasales.service.dto.PrivilegeResponse;
import ru.aviasales.service.dto.RolePrivilegeMutationRequest;
import ru.aviasales.service.dto.RolePrivilegesResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ModeratorService moderatorService;
    private final PrivilegeService privilegeService;

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('CAMPAIGN_VIEW_ALL')")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(moderatorService.getAllCampaigns());
    }

    @PostMapping("/managers")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<AccountResponse> createManager(@Valid @RequestBody ManagerRequest request) {
        return ResponseEntity.ok(adminService.createManager(request));
    }

    @DeleteMapping("/managers/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        adminService.deleteManager(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        adminService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/privileges")
    @PreAuthorize("hasAuthority('PRIVILEGE_VIEW')")
    public ResponseEntity<List<PrivilegeResponse>> listPrivileges() {
        return ResponseEntity.ok(privilegeService.listPrivileges());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PRIVILEGE_VIEW')")
    public ResponseEntity<List<RolePrivilegesResponse>> listRolePrivileges() {
        return ResponseEntity.ok(privilegeService.listRolePrivileges());
    }

    @PostMapping("/roles/{role}/privileges")
    @PreAuthorize("hasAuthority('PRIVILEGE_ASSIGN')")
    public ResponseEntity<RolePrivilegesResponse> mutateRolePrivilege(
            @PathVariable UserRole role,
            @Valid @RequestBody RolePrivilegeMutationRequest request) {
        return ResponseEntity.ok(privilegeService.mutate(role, request));
    }
}
