package ru.aviasales.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.CampaignStatus;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Comment;
import ru.aviasales.dal.model.Moderator;
import ru.aviasales.dal.repository.AdvertisingCampaignRepository;
import ru.aviasales.dal.repository.CampaignSignatureRepository;
import ru.aviasales.dal.repository.CommentRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.service.dto.CampaignResponse;
import ru.aviasales.service.dto.ModeratorActionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeratorServiceTest {

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private AdvertisingCampaignRepository campaignRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CampaignSignatureRepository campaignSignatureRepository;

    @InjectMocks
    private ModeratorService moderatorService;

    @Test
    void actionCampaignSignDocShouldSignByModeratorAndMoveToAtSigning() {
        String apiKey = "moderator-key";
        Long campaignId = 10L;

        Moderator moderator = new Moderator();
        moderator.setId(7L);

        Client client = new Client();
        client.setId(4L);

        AdvertisingCampaign campaign = new AdvertisingCampaign();
        campaign.setId(campaignId);
        campaign.setName("Campaign");
        campaign.setContent("Content");
        campaign.setTargetUrl("https://example.com");
        campaign.setDailyBudget(new BigDecimal("100.00"));
        campaign.setStartDate(LocalDateTime.of(2026, 3, 10, 12, 0));
        campaign.setEndDate(LocalDateTime.of(2026, 3, 15, 12, 0));
        campaign.setClient(client);
        campaign.setStatus(CampaignStatus.PENDING);

        ModeratorActionRequest request = new ModeratorActionRequest();
        request.setAction(ModeratorActionRequest.Action.SIGN_DOC);
        request.setComment("Signed by moderator");

        when(moderatorRepository.findByApiKey(apiKey)).thenReturn(Optional.of(moderator));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignSignatureRepository.findByCampaign(campaign)).thenReturn(Optional.empty());
        when(campaignSignatureRepository.save(any(CampaignSignature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        CampaignResponse response = moderatorService.actionCampaign(apiKey, campaignId, request);

        ArgumentCaptor<CampaignSignature> signatureCaptor = ArgumentCaptor.forClass(CampaignSignature.class);
        verify(campaignSignatureRepository).save(signatureCaptor.capture());
        CampaignSignature savedSignature = signatureCaptor.getValue();

        assertEquals(CampaignStatus.AT_SIGNING, campaign.getStatus());
        assertEquals(campaign, savedSignature.getCampaign());
        assertEquals(moderator.getId(), savedSignature.getModeratorId());
        assertNotNull(savedSignature.getModeratorSignedAt());
        assertNotNull(savedSignature.getDocumentHash());
        assertFalse(savedSignature.isFullySigned());

        assertEquals(savedSignature.getDocumentHash(), response.getDocumentHash());
        assertNotNull(response.getModeratorSignedAt());
        assertFalse(response.isFullySigned());
    }
}
