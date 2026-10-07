package com.geopslabs.geops.catalog.infrastructure.web;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(Long userId, Long businessId) {
    private static final String BUSINESS_ID_CLAIM = "businessId";

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(parseSubject(jwt.getSubject()), businessIdOf(jwt));
    }

    public Long requireBusinessId() {
        if (businessId == null) {
            throw new TokenClaimsException();
        }
        return businessId;
    }

    private static Long parseSubject(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new TokenClaimsException();
        }
    }

    private static Long businessIdOf(Jwt jwt) {
        Object claim = jwt.getClaim(BUSINESS_ID_CLAIM);
        return claim instanceof Number number ? number.longValue() : null;
    }
}
