package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.Privilege;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.dal.repository.PrivilegeRepository;
import ru.aviasales.dal.repository.RolePrivilegeRepository;
import ru.aviasales.security.PrivilegeCodes;
import ru.aviasales.security.PrivilegeResolver;
import ru.aviasales.service.dto.PrivilegeResponse;
import ru.aviasales.service.dto.RolePrivilegeMutationRequest;
import ru.aviasales.service.dto.RolePrivilegesResponse;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivilegeService {

    private static final Set<String> CLIENT_ALLOWED = Set.of(
            PrivilegeCodes.CAMPAIGN_CREATE,
            PrivilegeCodes.CAMPAIGN_UPDATE_OWN,
            PrivilegeCodes.CAMPAIGN_DELETE_OWN,
            PrivilegeCodes.CAMPAIGN_VIEW_OWN,
            PrivilegeCodes.CAMPAIGN_SIGN_CLIENT,
            PrivilegeCodes.CAMPAIGN_PAUSE_OWN,
            PrivilegeCodes.PAYMENT_TOPUP,
            PrivilegeCodes.PAYMENT_VIEW_OWN
    );

    private static final Set<String> MANAGER_ALLOWED = Set.of(
            PrivilegeCodes.CAMPAIGN_VIEW_ALL,
            PrivilegeCodes.CAMPAIGN_MODERATE_SIGN,
            PrivilegeCodes.CAMPAIGN_REJECT,
            PrivilegeCodes.CAMPAIGN_PAUSE_ANY,
            PrivilegeCodes.CAMPAIGN_DELETE_ANY,
            PrivilegeCodes.COMMENT_CREATE,
            PrivilegeCodes.COMMENT_DELETE_OWN,
            PrivilegeCodes.COMMENT_DELETE_ANY,
            PrivilegeCodes.CLIENT_DELETE_ANY
    );

    private static final Map<UserRole, Set<String>> ROLE_ALLOWLIST = Map.of(
            UserRole.CLIENT, CLIENT_ALLOWED,
            UserRole.MANAGER, MANAGER_ALLOWED
    );

    private final PrivilegeRepository privilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final PrivilegeResolver privilegeResolver;

    @Transactional(readOnly = true)
    public List<PrivilegeResponse> listPrivileges() {
        return privilegeRepository.findAll().stream()
                .map(PrivilegeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolePrivilegesResponse> listRolePrivileges() {
        Map<UserRole, List<String>> grouped = new EnumMap<>(UserRole.class);
        for (UserRole role : UserRole.values()) {
            grouped.put(role, new ArrayList<>());
        }
        for (Object[] row : privilegeRepository.findAllRolePrivilegeMappings()) {
            UserRole role = UserRole.valueOf((String) row[0]);
            grouped.get(role).add((String) row[1]);
        }
        List<RolePrivilegesResponse> result = new ArrayList<>(UserRole.values().length);
        for (UserRole role : UserRole.values()) {
            result.add(new RolePrivilegesResponse(role, grouped.get(role)));
        }
        return result;
    }

    @Transactional
    public RolePrivilegesResponse mutate(UserRole role, RolePrivilegeMutationRequest request) {
        if (role == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ADMIN role privileges are immutable to preserve break-glass access");
        }
        Set<String> allowList = ROLE_ALLOWLIST.get(role);
        if (allowList == null || !allowList.contains(request.getPrivilegeCode())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Privilege " + request.getPrivilegeCode() + " is not assignable to role " + role);
        }

        Privilege privilege = privilegeRepository.findByCode(request.getPrivilegeCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Privilege not found"));

        String actor = currentActor();
        switch (request.getOperation()) {
            case GRANT -> {
                rolePrivilegeRepository.grant(role, privilege.getId());
                log.info("audit: actor={} GRANT privilege={} to role={}", actor, privilege.getCode(), role);
            }
            case REVOKE -> {
                rolePrivilegeRepository.revoke(role, privilege.getId());
                log.info("audit: actor={} REVOKE privilege={} from role={}", actor, privilege.getCode(), role);
            }
        }

        privilegeResolver.invalidate(role);
        return new RolePrivilegesResponse(role,
                privilegeRepository.findCodesByRole(role).stream().sorted().toList());
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }
}
