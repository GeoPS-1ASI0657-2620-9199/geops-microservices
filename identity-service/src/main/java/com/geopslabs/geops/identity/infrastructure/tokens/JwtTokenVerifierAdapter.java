package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.TokenFingerprint;
import com.geopslabs.geops.identity.domain.models.VerifiedToken;
import com.geopslabs.geops.identity.domain.ports.TokenVerifierPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Checks a token with the same decoder that protects the profile routes of Identity: RS256 signature with its public
 * key, issuer, audience and expiry. A token whose roles claim is not exactly one known role is not accepted.
 */
@Component
public class JwtTokenVerifierAdapter implements TokenVerifierPort {
    private static final String ROLES_CLAIM = "roles";
    private static final String BUSINESS_ID_CLAIM = "businessId";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtDecoder jwtDecoder;

    public JwtTokenVerifierAdapter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Optional<VerifiedToken> verify(String token) {
        try {
            return toVerifiedToken(token, jwtDecoder.decode(token));
        } catch (JwtException exception) {
            return Optional.empty();
        }
    }

    private static Optional<VerifiedToken> toVerifiedToken(String token, Jwt jwt) {
        var role = roleOf(jwt.getClaimAsStringList(ROLES_CLAIM));
        var userId = numberOrNull(jwt.getSubject());
        if (role.isEmpty() || userId == null || jwt.getExpiresAt() == null) {
            return Optional.empty();
        }
        Number businessId = jwt.getClaim(BUSINESS_ID_CLAIM);
        return Optional.of(new VerifiedToken(TokenFingerprint.of(token), userId, role.get(),
                businessId == null ? null : businessId.longValue(), jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    private static Optional<Role> roleOf(List<String> roles) {
        if (roles == null || roles.size() != 1 || !roles.get(0).startsWith(ROLE_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(roles.get(0).substring(ROLE_PREFIX.length())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Long numberOrNull(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
