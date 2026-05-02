package ru.aviasales.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.aviasales.dal.model.UserRole;

import java.util.List;

@Data
@AllArgsConstructor
public class RolePrivilegesResponse {
    private UserRole role;
    private List<String> privileges;
}
