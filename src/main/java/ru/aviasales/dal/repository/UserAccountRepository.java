package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.UserAccount;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("SELECT u FROM UserAccount u " +
            "LEFT JOIN FETCH u.client " +
            "LEFT JOIN FETCH u.moderator " +
            "WHERE u.username = :username")
    Optional<UserAccount> findByUsernameWithOwners(@Param("username") String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserAccount u " +
            "LEFT JOIN FETCH u.client " +
            "LEFT JOIN FETCH u.moderator " +
            "WHERE u.client.id = :clientId")
    List<UserAccount> findByClientIdWithOwners(@Param("clientId") Long clientId);

    @Query("SELECT u FROM UserAccount u " +
            "LEFT JOIN FETCH u.client " +
            "LEFT JOIN FETCH u.moderator " +
            "WHERE u.moderator.id = :moderatorId")
    List<UserAccount> findByModeratorIdWithOwners(@Param("moderatorId") Long moderatorId);
}
