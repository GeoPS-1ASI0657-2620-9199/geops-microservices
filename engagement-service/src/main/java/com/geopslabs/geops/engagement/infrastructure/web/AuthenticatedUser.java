package com.geopslabs.geops.engagement.infrastructure.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** {@code fullName} is the optional "name" claim of Identity; a token issued before it existed has none. */
public record AuthenticatedUser(Long consumerId, String fullName) {
    private static final String NAME_CLAIM = "name";

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(parseSubject(jwt.getSubject()), jwt.getClaimAsString(NAME_CLAIM));
    }

    private static Long parseSubject(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new TokenClaimsException();
        }
    }
}
