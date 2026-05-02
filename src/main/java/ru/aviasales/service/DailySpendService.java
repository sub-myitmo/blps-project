package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Transaction;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailySpendService {

    private final AdvertisingCampaignRepository campaignRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    // Self-injection so that calls to @Transactional methods on `this` go through the AOP proxy
    // and start the configured propagation (REQUIRES_NEW per client). Without this the @Transactional
    // would be ignored when invoked from the same instance.
    @Autowired
    @Lazy
    private DailySpendService self;

    @Scheduled(cron = "0 0 0 * * *")
    public void processDailySpending() {
        log.info("Starting daily spending processing");

        LocalDate today = LocalDate.now();

        self.startScheduledCampaigns(today);
        self.completeExpiredCampaigns(today);

        List<Long> clientIds = self.loadActiveCampaignClientIds();

        for (Long clientId : clientIds) {
            try {
                self.processClientDailySpending(clientId);
            } catch (RuntimeException ex) {
                log.error("Failed daily spending for client id={}", clientId, ex);
            }
        }

        log.info("Daily spending processing completed");
    }

    @Transactional(readOnly = true)
    public List<Long> loadActiveCampaignClientIds() {
        return campaignRepository.findClientIdsByStatus(CampaignStatus.ACTIVE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startScheduledCampaigns(LocalDate today) {
        List<AdvertisingCampaign> readyToStart = campaignRepository.findReadyToStart(today);

        for (AdvertisingCampaign campaign : readyToStart) {
            if (campaign.getClient().getBalance().compareTo(campaign.getDailyBudget()) >= 0) {
                campaign.transitionTo(CampaignStatus.ACTIVE);
                campaignRepository.save(campaign);
                log.info("Campaign id={} started", campaign.getId());
            } else {
                campaign.transitionTo(CampaignStatus.FROZEN);
                campaignRepository.save(campaign);
                log.info("Campaign id={} frozen due to insufficient funds", campaign.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeExpiredCampaigns(LocalDate today) {
        List<AdvertisingCampaign> expired = campaignRepository.findExpiredActive(today);

        for (AdvertisingCampaign campaign : expired) {
            campaign.transitionTo(CampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            log.info("Campaign id={} completed", campaign.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processClientDailySpending(Long clientId) {
        Client client = clientRepository.findActiveByIdForUpdate(clientId).orElse(null);
        if (client == null) {
            log.warn("Client id={} not found or deleted; skipping", clientId);
            return;
        }

        // Re-query inside the locked transaction — the snapshot from loadActiveCampaignClientIds
        // may be stale (campaigns frozen, completed, or deleted between phases).
        List<AdvertisingCampaign> campaigns = campaignRepository
                .findByStatusAndClientId(CampaignStatus.ACTIVE, clientId);
        if (campaigns.isEmpty()) {
            return;
        }

        BigDecimal totalToSpend = campaigns.stream()
                .map(AdvertisingCampaign::getDailyBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (client.getBalance().compareTo(totalToSpend) >= 0) {
            client.setBalance(client.getBalance().subtract(totalToSpend));
            clientRepository.save(client);

            Transaction transaction = new Transaction();
            transaction.setClientId(client.getId());
            transaction.setAmount(totalToSpend.negate());
            transaction.setType(Transaction.TransactionType.DAILY_DEBIT);
            transaction.setDescription("Daily spending for " + campaigns.size() + " campaigns");
            transactionRepository.save(transaction);

            log.info("Client id={} debited {}. New balance: {}",
                    client.getId(), totalToSpend, client.getBalance());
        } else {
            log.warn("Client id={} insufficient funds. Freezing campaigns.", client.getId());
            campaignRepository.freezeAllClientCampaigns(client.getId());
        }
    }
}
