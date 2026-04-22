package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.CampaignSignature;
import ru.aviasales.dal.model.PdfStatus;

import java.time.Instant;

@Data
public class CampaignSignatureDetailsResponse {
    private Long campaignId;
    private String documentHash;
    private String hashAlgorithm;
    private String documentTemplateVersion;
    private String documentSnapshot;
    private Long moderatorId;
    private Instant moderatorSignedAtUtc;
    private Long clientId;
    private Instant clientSignedAtUtc;
    private boolean fullySigned;
    private Instant fullySignedAtUtc;
    private String pdfObjectKey;
    private String pdfContentType;
    private Instant pdfCreatedAtUtc;
    private PdfStatus pdfStatus;

    public static CampaignSignatureDetailsResponse fromEntity(CampaignSignature signature) {
        CampaignSignatureDetailsResponse response = new CampaignSignatureDetailsResponse();
        response.setCampaignId(signature.getCampaign() != null ? signature.getCampaign().getId() : null);
        response.setDocumentHash(signature.getDocumentHash());
        response.setHashAlgorithm(signature.getHashAlgorithm());
        response.setDocumentTemplateVersion(signature.getDocumentTemplateVersion());
        response.setDocumentSnapshot(signature.getDocumentSnapshot());
        response.setModeratorId(signature.getModeratorId());
        response.setModeratorSignedAtUtc(signature.getModeratorSignedAtUtc());
        response.setClientId(signature.getClientId());
        response.setClientSignedAtUtc(signature.getClientSignedAtUtc());
        response.setFullySigned(signature.isFullySigned());
        response.setFullySignedAtUtc(signature.getFullySignedAtUtc());
        response.setPdfObjectKey(signature.getPdfObjectKey());
        response.setPdfContentType(signature.getPdfContentType());
        response.setPdfCreatedAtUtc(signature.getPdfCreatedAtUtc());
        response.setPdfStatus(signature.getPdfStatus());
        return response;
    }
}
