package com.geopslabs.geops.engagement.infrastructure.web;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(Long consumerId) {

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(parseSubject(jwt.getSubject()));
    }

    private static Long parseSubject(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new TokenClaimsException();
        }
    }
}
