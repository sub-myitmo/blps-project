package ru.aviasales.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RolePrivilegeMutationRequest {

    public enum Operation { GRANT, REVOKE }

    @NotBlank
    @Size(max = 64)
    private String privilegeCode;

    @NotNull
    private Operation operation;
}
