package ru.aviasales.service.edo;

public final class EdoDocumentStatus {
    public static final String RECIPIENT_SIGNATURE_REQUIRED = "RECIPIENT_SIGNATURE_REQUIRED";
    public static final String COUNTERSIGN_INITIATED = "COUNTERSIGN_INITIATED";
    public static final String COMPLETED = "COMPLETED";
    /** Legacy / provider-specific status: sender (moderator) has signed on the operator side. */
    public static final String SENDER_SIGNED = "SENDER_SIGNED";
    /** Legacy / provider-specific status: recipient (client) has signed on the operator side. */
    public static final String RECIPIENT_SIGNED = "RECIPIENT_SIGNED";

    private EdoDocumentStatus() {
    }

    public static boolean isModeratorConfirmed(String status) {
        return RECIPIENT_SIGNATURE_REQUIRED.equals(status)
                || COUNTERSIGN_INITIATED.equals(status)
                || COMPLETED.equals(status)
                || SENDER_SIGNED.equals(status)
                || RECIPIENT_SIGNED.equals(status);
    }

    public static boolean canInitiateCountersign(String status) {
        return RECIPIENT_SIGNATURE_REQUIRED.equals(status)
                || SENDER_SIGNED.equals(status);
    }

    public static boolean isCountersignInProgress(String status) {
        return COUNTERSIGN_INITIATED.equals(status)
                || RECIPIENT_SIGNED.equals(status);
    }

    public static boolean isCompleted(String status) {
        return COMPLETED.equals(status);
    }
}
