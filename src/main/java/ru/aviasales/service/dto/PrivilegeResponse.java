package ru.aviasales.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.aviasales.dal.model.Privilege;

@Data
@AllArgsConstructor
public class PrivilegeResponse {
    private Long id;
    private String code;
    private String description;

    public static PrivilegeResponse fromEntity(Privilege privilege) {
        return new PrivilegeResponse(privilege.getId(), privilege.getCode(), privilege.getDescription());
    }
}
