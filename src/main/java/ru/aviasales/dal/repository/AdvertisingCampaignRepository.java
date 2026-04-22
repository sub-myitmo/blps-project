package ru.aviasales.dal.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvertisingCampaignRepository extends JpaRepository<AdvertisingCampaign, Long> {

    @Query("SELECT DISTINCT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.comments " +
            "LEFT JOIN FETCH c.signature " +
            "WHERE c.client.id = :clientId")
    List<AdvertisingCampaign> findByClientIdWithDetails(@Param("clientId") Long clientId);

    @Query("SELECT DISTINCT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.comments " +
            "LEFT JOIN FETCH c.signature " +
            "WHERE c.status = :status")
    List<AdvertisingCampaign> findByStatusWithDetails(@Param("status") CampaignStatus status);

    @Query("SELECT DISTINCT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.comments " +
            "LEFT JOIN FETCH c.signature")
    List<AdvertisingCampaign> findAllWithDetails();

    @Query("SELECT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "WHERE c.status = :status")
    List<AdvertisingCampaign> findByStatusWithClient(@Param("status") CampaignStatus status);

    @Query("SELECT DISTINCT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.comments " +
            "LEFT JOIN FETCH c.signature " +
            "WHERE c.id = :id")
    Optional<AdvertisingCampaign> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.comments " +
            "LEFT JOIN FETCH c.signature " +
            "WHERE c.id = :id")
    Optional<AdvertisingCampaign> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT c FROM AdvertisingCampaign c " +
            "JOIN FETCH c.client " +
            "WHERE c.status = 'WAITING_START' " +
            "AND (c.startDate IS NULL OR c.startDate <= :today) " +
            "AND (c.endDate IS NULL OR c.endDate >= :today)")
    List<AdvertisingCampaign> findReadyToStart(@Param("today") LocalDate today);

    @Query("SELECT c FROM AdvertisingCampaign c WHERE c.status = 'ACTIVE' " +
            "AND (c.endDate IS NOT NULL AND c.endDate < :today)")
    List<AdvertisingCampaign> findExpiredActive(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE AdvertisingCampaign c SET c.status = 'FROZEN' " +
            "WHERE c.client.id = :clientId " +
            "AND c.status = 'ACTIVE'")
    void freezeAllClientCampaigns(@Param("clientId") Long clientId);
}
