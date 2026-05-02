package ru.aviasales.security;

public final class PrivilegeCodes {

    private PrivilegeCodes() {
    }

    public static final String CAMPAIGN_CREATE = "CAMPAIGN_CREATE";
    public static final String CAMPAIGN_UPDATE_OWN = "CAMPAIGN_UPDATE_OWN";
    public static final String CAMPAIGN_DELETE_OWN = "CAMPAIGN_DELETE_OWN";
    public static final String CAMPAIGN_VIEW_OWN = "CAMPAIGN_VIEW_OWN";
    public static final String CAMPAIGN_SIGN_CLIENT = "CAMPAIGN_SIGN_CLIENT";
    public static final String CAMPAIGN_PAUSE_OWN = "CAMPAIGN_PAUSE_OWN";

    public static final String PAYMENT_TOPUP = "PAYMENT_TOPUP";
    public static final String PAYMENT_VIEW_OWN = "PAYMENT_VIEW_OWN";

    public static final String CAMPAIGN_VIEW_ALL = "CAMPAIGN_VIEW_ALL";
    public static final String CAMPAIGN_MODERATE_SIGN = "CAMPAIGN_MODERATE_SIGN";
    public static final String CAMPAIGN_REJECT = "CAMPAIGN_REJECT";
    public static final String CAMPAIGN_PAUSE_ANY = "CAMPAIGN_PAUSE_ANY";
    public static final String CAMPAIGN_DELETE_ANY = "CAMPAIGN_DELETE_ANY";
    public static final String COMMENT_CREATE = "COMMENT_CREATE";
    public static final String COMMENT_DELETE_OWN = "COMMENT_DELETE_OWN";
    public static final String COMMENT_DELETE_ANY = "COMMENT_DELETE_ANY";
    public static final String CLIENT_DELETE_ANY = "CLIENT_DELETE_ANY";

    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String PRIVILEGE_VIEW = "PRIVILEGE_VIEW";
    public static final String PRIVILEGE_ASSIGN = "PRIVILEGE_ASSIGN";
}
