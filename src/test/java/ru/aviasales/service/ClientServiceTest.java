package ru.aviasales.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ClientActionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AdvertisingCampaignRepository campaignRepository;

    @Mock
    private CampaignSignatureRepository campaignSignatureRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void actionCampaignSignDocShouldSignByClientAfterModeratorAndMoveToWaitingStart() {
        String apiKey = "client-key";
        Long campaignId = 22L;

        Client client = new Client();
        client.setId(5L);
        client.setApiKey(apiKey);

        AdvertisingCampaign campaign = buildCampaign(client);
        campaign.setStatus(CampaignStatus.AT_SIGNING);

        CampaignSignature signature = new CampaignSignature();
        signature.setCampaign(campaign);
        signature.setDocumentHash(CampaignDocumentHashUtil.buildDocumentHash(campaign));
        signature.setModeratorId(17L);
        signature.setModeratorSignedAt(LocalDateTime.now().minusHours(1));
        signature.setFullySigned(false);

        ClientActionRequest request = new ClientActionRequest();
        request.setAction(ClientActionRequest.ClientAction.SIGN_DOC);

        when(clientRepository.findByApiKey(apiKey)).thenReturn(Optional.of(client));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignSignatureRepository.findByCampaign(campaign)).thenReturn(Optional.of(signature));
        when(campaignSignatureRepository.save(any(CampaignSignature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        CampaignResponse response = clientService.actionCampaign(apiKey, campaignId, request);

        assertEquals(CampaignStatus.WAITING_START, campaign.getStatus());
        assertEquals(client.getId(), signature.getClientId());
        assertNotNull(signature.getClientSignedAt());
        assertTrue(signature.isFullySigned());

        assertTrue(response.isFullySigned());
        assertNotNull(response.getClientSignedAt());
    }

    @Test
    void actionCampaignSignDocShouldFailIfModeratorDidNotSign() {
        String apiKey = "client-key";
        Long campaignId = 23L;

        Client client = new Client();
        client.setId(5L);
        client.setApiKey(apiKey);

        AdvertisingCampaign campaign = buildCampaign(client);
        campaign.setStatus(CampaignStatus.AT_SIGNING);

        ClientActionRequest request = new ClientActionRequest();
        request.setAction(ClientActionRequest.ClientAction.SIGN_DOC);

        when(clientRepository.findByApiKey(apiKey)).thenReturn(Optional.of(client));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignSignatureRepository.findByCampaign(campaign)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clientService.actionCampaign(apiKey, campaignId, request));

        assertEquals("Campaign is not signed by moderator", exception.getMessage());
        verify(campaignRepository, never()).save(any(AdvertisingCampaign.class));
    }

    @Test
    void actionCampaignSignDocShouldFailForInvalidCampaignStatus() {
        String apiKey = "client-key";
        Long campaignId = 24L;

        Client client = new Client();
        client.setId(5L);
        client.setApiKey(apiKey);

        AdvertisingCampaign campaign = buildCampaign(client);
        campaign.setStatus(CampaignStatus.PENDING);

        ClientActionRequest request = new ClientActionRequest();
        request.setAction(ClientActionRequest.ClientAction.SIGN_DOC);

        when(clientRepository.findByApiKey(apiKey)).thenReturn(Optional.of(client));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clientService.actionCampaign(apiKey, campaignId, request));

        assertEquals("Campaign must be in AT_SIGNING status for client signing", exception.getMessage());
        verify(campaignSignatureRepository, never()).findByCampaign(any(AdvertisingCampaign.class));
        verify(campaignRepository, never()).save(any(AdvertisingCampaign.class));
    }

    private AdvertisingCampaign buildCampaign(Client client) {
        AdvertisingCampaign campaign = new AdvertisingCampaign();
        campaign.setId(100L);
        campaign.setName("Campaign");
        campaign.setContent("Content");
        campaign.setTargetUrl("https://example.com");
        campaign.setDailyBudget(new BigDecimal("10.00"));
        campaign.setStartDate(LocalDateTime.of(2026, 3, 12, 0, 0));
        campaign.setEndDate(LocalDateTime.of(2026, 3, 20, 0, 0));
        campaign.setClient(client);
        return campaign;
    }
}
