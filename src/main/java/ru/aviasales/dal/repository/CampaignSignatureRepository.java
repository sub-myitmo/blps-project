package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;

import java.util.Optional;

@Repository
public interface CampaignSignatureRepository extends JpaRepository<CampaignSignature, Long> {
    Optional<CampaignSignature> findByCampaign(AdvertisingCampaign campaign);

    @Query("""
            select distinct signature
            from CampaignSignature signature
            left join fetch signature.auditEvents
            where signature.campaign = :campaign
            """)
    Optional<CampaignSignature> findDetailedByCampaign(@Param("campaign") AdvertisingCampaign campaign);
}
