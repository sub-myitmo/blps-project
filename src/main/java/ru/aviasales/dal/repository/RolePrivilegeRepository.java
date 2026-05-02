package ru.aviasales.dal.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.UserRole;

@Repository
@RequiredArgsConstructor
public class RolePrivilegeRepository {

    private final EntityManager entityManager;

    public int grant(UserRole role, Long privilegeId) {
        return entityManager.createNativeQuery(
                        "INSERT INTO role_privileges (role, privilege_id) VALUES (:role, :privilegeId) " +
                                "ON CONFLICT DO NOTHING")
                .setParameter("role", role.name())
                .setParameter("privilegeId", privilegeId)
                .executeUpdate();
    }

    public int revoke(UserRole role, Long privilegeId) {
        return entityManager.createNativeQuery(
                        "DELETE FROM role_privileges WHERE role = :role AND privilege_id = :privilegeId")
                .setParameter("role", role.name())
                .setParameter("privilegeId", privilegeId)
                .executeUpdate();
    }
}
