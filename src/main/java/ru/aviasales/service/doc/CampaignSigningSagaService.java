package ru.aviasales.service.doc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.aviasales.dal.model.AdvertisingCampaign;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.PdfStatus;
import ru.aviasales.dal.repository.CampaignSignatureRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignSigningSagaService {

    private final CampaignSignatureRepository campaignSignatureRepository;
    private final CampaignDocumentPdfService campaignDocumentPdfService;
    private final PdfStorageService pdfStorageService;

    public CampaignSignature restartSigning(AdvertisingCampaign campaign, Long moderatorId) {
        CampaignSignature signature = campaignSignatureRepository.findByCampaignId(campaign.getId())
                .orElseGet(() -> {
                    CampaignSignature newSignature = new CampaignSignature();
                    newSignature.setCampaign(campaign);
                    return newSignature;
                });

        String documentSnapshot = CampaignSigningDocumentFactory.buildSnapshot(campaign);
        String documentHash = CampaignDocumentHashUtil.buildDocumentHash(documentSnapshot);
        signature.setDocumentHash(documentHash);
        signature.setHashAlgorithm(CampaignDocumentHashUtil.HASH_ALGORITHM);
        signature.setDocumentTemplateVersion(CampaignSigningDocumentFactory.TEMPLATE_VERSION);
        signature.setDocumentSnapshot(documentSnapshot);

        signature.setModeratorId(moderatorId);
        signature.setModeratorSignedAtUtc(moderatorId != null ? Instant.now() : null);
        signature.setClientId(null);
        signature.setClientSignedAtUtc(null);
        signature.setFullySigned(false);
        signature.setFullySignedAtUtc(null);

        CampaignSignature saved = storePdf(signature);
        campaign.setSignature(saved);
        return saved;
    }

    public CampaignSignature ensurePdfStored(CampaignSignature signature) {
        if (signature.getPdfStatus() == PdfStatus.READY
                && signature.getPdfObjectKey() != null
                && !signature.getPdfObjectKey().isBlank()) {
            return signature;
        }

        return storePdf(signature);
    }

    public CampaignSignature regeneratePdf(CampaignSignature signature) {
        return storePdf(signature);
    }

    private CampaignSignature storePdf(CampaignSignature signature) {
        String oldObjectKey = signature.getPdfObjectKey();
        String newObjectKey = "campaigns/" + signature.getCampaign().getId()
                + "/signing-document-" + UUID.randomUUID() + ".pdf";

        signature.setPdfObjectKey(newObjectKey);
        signature.setPdfContentType(PdfStorageService.PDF_CONTENT_TYPE);
        signature.setPdfCreatedAtUtc(Instant.now());
        signature.setPdfStatus(PdfStatus.READY);

        byte[] pdf = campaignDocumentPdfService.generatePdf(signature);
        pdfStorageService.uploadPdf(newObjectKey, pdf);
        registerCompensation(newObjectKey, oldObjectKey);

        return campaignSignatureRepository.save(signature);
    }

    private void registerCompensation(String newObjectKey, String oldObjectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    pdfStorageService.deleteObjectQuietly(newObjectKey);
                } else if (oldObjectKey != null && !oldObjectKey.equals(newObjectKey)) {
                    pdfStorageService.deleteObjectQuietly(oldObjectKey);
                }
            }
        });
    }
}
