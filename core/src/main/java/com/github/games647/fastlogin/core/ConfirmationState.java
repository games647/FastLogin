package com.github.games647.fastlogin.core;

public enum ConfirmationState {

    /**
     * Require server login where we request onlinemode authentication
     */
    REQUIRE_RELOGIN,

    /**
     * The command have to be invoked again to confirm that the player who joined through onlinemode knows
     * the password of the cracked account
     */
    REQUIRE_AUTH_PLUGIN_LOGIN
}
