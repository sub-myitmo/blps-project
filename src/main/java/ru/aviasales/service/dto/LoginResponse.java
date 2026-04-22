package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.security.UserPrincipal;

import java.time.Instant;

@Data
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private String username;
    private UserRole role;
    private Long clientId;
    private Long moderatorId;

    public static LoginResponse fromToken(String token, Instant expiresAt, UserPrincipal principal) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresAt(expiresAt);
        response.setUsername(principal.getUsername());
        response.setRole(principal.getRole());
        response.setClientId(principal.getClientId());
        response.setModeratorId(principal.getModeratorId());
        return response;
    }
}
