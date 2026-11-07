package com.geopslabs.geops.notification.domain.models;

import com.geopslabs.geops.notification.domain.models.exceptions.InvalidUnsubscribeTokenException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public record UnsubscribeToken(Long consumerId, NotificationType type) {
    private static final int SIGNATURE_LENGTH = 22;
    private static final int MAX_TOKEN_LENGTH = 64;
    private static final String ALGORITHM = "HmacSHA256";
    private static final Set<NotificationType> OFFER_TYPES = Set.of(NotificationType.NEARBY_OFFER, NotificationType.FOLLOWED_BUSINESS_OFFER);

    public static String issue(Long consumerId, NotificationType type, String secret) {
        if (consumerId == null || consumerId <= 0 || !OFFER_TYPES.contains(type)) throw new InvalidUnsubscribeTokenException();
        var content = consumerId + "." + type.name();
        var token = content + "." + signature(content, secret);
        if (token.length() > MAX_TOKEN_LENGTH) throw new InvalidUnsubscribeTokenException();
        return token;
    }

    public static UnsubscribeToken verify(String token, String secret) {
        try {
            if (token == null || token.length() > MAX_TOKEN_LENGTH) throw new InvalidUnsubscribeTokenException();
            var parts = token.split("\\.", -1);
            if (parts.length != 3) throw new InvalidUnsubscribeTokenException();
            var consumerId = Long.valueOf(parts[0]);
            var type = NotificationType.valueOf(parts[1]);
            var expected = issue(consumerId, type, secret);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), token.getBytes(StandardCharsets.US_ASCII))) throw new InvalidUnsubscribeTokenException();
            return new UnsubscribeToken(consumerId, type);
        } catch (IllegalArgumentException error) { throw new InvalidUnsubscribeTokenException(); }
    }

    private static String signature(String content, String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("Notification unsubscribe secret is not configured");
        try {
            var mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8))).substring(0, SIGNATURE_LENGTH);
        } catch (GeneralSecurityException error) { throw new IllegalStateException(error); }
    }
}
