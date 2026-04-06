package ru.aviasales.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;

import java.util.Optional;

@Repository
public interface CampaignSignatureRepository extends JpaRepository<CampaignSignature, Long> {
    Optional<CampaignSignature> findByCampaign(AdvertisingCampaign campaign);
}
