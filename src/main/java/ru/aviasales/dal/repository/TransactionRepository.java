package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByClient(Client client);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.client = :client AND t.type = 'DEPOSIT'")
    BigDecimal getTotalDeposits(@Param("client") Client client);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.client = :client AND t.type = 'DAILY_DEBIT' " +
            "AND t.createdAt >= :startOfDay")
    BigDecimal getTodaySpent(@Param("client") Client client,
                             @Param("startOfDay") LocalDateTime startOfDay);
}
