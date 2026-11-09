package com.geopslabs.geops.identity.infrastructure.web;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The caller as the token describes it: the subject is the user id and businessId comes only for a business owner.
 */
public record AuthenticatedUser(Long userId, Long businessId) {
    private static final String BUSINESS_ID_CLAIM = "businessId";

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(parseSubject(jwt.getSubject()), businessIdOf(jwt));
    }

    public Long consumerId() {
        return userId;
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
