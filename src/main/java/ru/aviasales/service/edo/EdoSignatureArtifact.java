package ru.aviasales.service.edo;

/**
 * A real signature artifact fetched from the ЭДО operator.
 * This represents actual cryptographic evidence — e.g. a CMS/PKCS#7 detached
 * signature, a signed PDF, or whatever the operator issues.
 *
 * @param content     raw artifact bytes
 * @param contentType MIME type (e.g. "application/pkcs7-signature", "application/pdf")
 * @param filename    suggested download filename (e.g. "signature.p7s")
 */
public record EdoSignatureArtifact(
        byte[] content,
        String contentType,
        String filename
) {}
