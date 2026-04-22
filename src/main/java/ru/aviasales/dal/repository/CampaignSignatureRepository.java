package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.CampaignSignature;

import java.util.Optional;

@Repository
public interface CampaignSignatureRepository extends JpaRepository<CampaignSignature, Long> {

    @Query("SELECT s FROM CampaignSignature s " +
            "JOIN FETCH s.campaign " +
            "WHERE s.campaign.id = :campaignId")
    Optional<CampaignSignature> findByCampaignId(@Param("campaignId") Long campaignId);
}
