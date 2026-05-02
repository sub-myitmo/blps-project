package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.Privilege;
import ru.aviasales.dal.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByCode(String code);

    @Query(value = "SELECT p.code FROM privileges p " +
            "JOIN role_privileges rp ON rp.privilege_id = p.id " +
            "WHERE rp.role = :role", nativeQuery = true)
    Set<String> findCodesByRole(@Param("role") String role);

    @Query(value = "SELECT rp.role, p.code FROM privileges p " +
            "JOIN role_privileges rp ON rp.privilege_id = p.id " +
            "ORDER BY rp.role, p.code", nativeQuery = true)
    List<Object[]> findAllRolePrivilegeMappings();

    default Set<String> findCodesByRole(UserRole role) {
        return findCodesByRole(role.name());
    }
}
