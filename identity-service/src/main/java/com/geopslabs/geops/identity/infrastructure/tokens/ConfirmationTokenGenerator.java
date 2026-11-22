package com.geopslabs.geops.identity.infrastructure.tokens;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;

/** 32 random bytes in URL-safe Base64: the token goes in the query string of the confirmation link. */
public class ConfirmationTokenGenerator implements Supplier<String> {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String get() {
        var bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
