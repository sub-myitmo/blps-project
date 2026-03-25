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
import ru.aviasales.dal.model.Client;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AdvertisingCampaignRepository extends JpaRepository<AdvertisingCampaign, Long> {

    List<AdvertisingCampaign> findByClient(Client client);

    List<AdvertisingCampaign> findByStatus(CampaignStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AdvertisingCampaign c WHERE c.id = :id")
    java.util.Optional<AdvertisingCampaign> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT c FROM AdvertisingCampaign c WHERE c.status = 'WAITING_START' " +
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
