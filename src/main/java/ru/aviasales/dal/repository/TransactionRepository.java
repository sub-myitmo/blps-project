package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByClientId(Long clientId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.clientId = :clientId AND t.type = 'DEPOSIT'")
    BigDecimal getTotalDeposits(@Param("clientId") Long clientId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.clientId = :clientId AND t.type = 'DAILY_DEBIT' " +
            "AND t.createdAt >= :startOfDay")
    BigDecimal getTodaySpent(@Param("clientId") Long clientId,
                             @Param("startOfDay") LocalDateTime startOfDay);
}
