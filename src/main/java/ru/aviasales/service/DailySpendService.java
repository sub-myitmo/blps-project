package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailySpendService {

    private final AdvertisingCampaignRepository campaignRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(cron = "0 0 0 * * *") // Каждый день в полночь
    @Transactional
    public void processDailySpending() {
        log.info("Starting daily spending processing");

        LocalDate today = LocalDate.now();

        startScheduledCampaigns(today);
        completeExpiredCampaigns(today);

        List<AdvertisingCampaign> activeCampaigns = campaignRepository
                .findByStatus(CampaignStatus.ACTIVE);

        // Группируем по клиентам
        Map<Client, List<AdvertisingCampaign>> campaignsByClient = activeCampaigns.stream()
                .collect(Collectors.groupingBy(AdvertisingCampaign::getClient));

        for (Map.Entry<Client, List<AdvertisingCampaign>> entry : campaignsByClient.entrySet()) {
            Client client = entry.getKey();
            List<AdvertisingCampaign> campaigns = entry.getValue();

            BigDecimal totalToSpend = campaigns.stream()
                    .map(AdvertisingCampaign::getDailyBudget)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (client.getBalance().compareTo(totalToSpend) >= 0) {
                client.setBalance(client.getBalance().subtract(totalToSpend));
                clientRepository.save(client);

                Transaction transaction = new Transaction();
                transaction.setClient(client);
                transaction.setAmount(totalToSpend.negate());
                transaction.setType(Transaction.TransactionType.DAILY_DEBIT);
                transaction.setDescription("Daily spending for " + campaigns.size() + " campaigns");
                transactionRepository.save(transaction);

                log.info("Client {} spent {}. New balance: {}",
                        client.getApiKey(), totalToSpend, client.getBalance());
            } else {
                log.warn("Client {} has insufficient funds. Freezing all campaigns.",
                        client.getApiKey());
                campaignRepository.freezeAllClientCampaigns(client.getId());
            }
        }

        log.info("Daily spending processing completed");
    }

    private void startScheduledCampaigns(LocalDate today) {
        List<AdvertisingCampaign> readyToStart = campaignRepository.findReadyToStart(today);

        for (AdvertisingCampaign campaign : readyToStart) {
            if (campaign.getClient().getBalance().compareTo(campaign.getDailyBudget()) >= 0) {
                campaign.transitionTo(CampaignStatus.ACTIVE);
                campaignRepository.save(campaign);
                log.info("Campaign {} started", campaign.getName());
            } else {
                campaign.transitionTo(CampaignStatus.FROZEN);
                campaignRepository.save(campaign);
                log.info("Campaign {} frozen due to insufficient funds", campaign.getName());
            }
        }
    }

    private void completeExpiredCampaigns(LocalDate today) {
        List<AdvertisingCampaign> expired = campaignRepository.findExpiredActive(today);

        for (AdvertisingCampaign campaign : expired) {
            campaign.transitionTo(CampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            log.info("Campaign {} completed", campaign.getName());
        }
    }
}
