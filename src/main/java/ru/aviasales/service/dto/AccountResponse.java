package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.model.UserRole;

@Data
public class AccountResponse {
    private Long id;
    private String username;
    private UserRole role;
    private boolean enabled;
    private Long clientId;
    private Long moderatorId;

    public static AccountResponse fromEntity(UserAccount account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setRole(account.getRole());
        response.setEnabled(account.isEnabled());
        response.setClientId(account.getClient() != null ? account.getClient().getId() : null);
        response.setModeratorId(account.getModerator() != null ? account.getModerator().getId() : null);
        return response;
    }
}
