package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.geopslabs.geops.identity.domain.models.IssuedToken;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class RsaTokenIssuerAdapter implements TokenIssuerPort {
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String BUSINESS_ID_CLAIM = "businessId";

    private final RsaKeyProvider keyProvider;
    private final JWSSigner signer;
    private final JwtProperties properties;
    private final Clock clock;

    public RsaTokenIssuerAdapter(RsaKeyProvider keyProvider, JwtProperties properties, Clock clock) {
        this.keyProvider = keyProvider;
        this.signer = signerFor(keyProvider);
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user, Long businessId) {
        var claims = claimsFor(user, businessId);
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keyProvider.signingKey().getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
        return new IssuedToken(sign(new SignedJWT(header, claims)), TOKEN_LIFETIME);
    }

    @Override
    public Map<String, Object> publicKeys() {
        return keyProvider.publicJwkSet();
    }

    private JWTClaimsSet claimsFor(User user, Long businessId) {
        var issuedAt = clock.instant();
        var builder = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .audience(properties.audience())
                .subject(String.valueOf(user.getId()))
                .claim(ROLES_CLAIM, List.of(ROLE_PREFIX + user.getRole().name()))
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(TOKEN_LIFETIME)));
        if (user.getRole() == Role.BUSINESS_OWNER) {
            builder.claim(BUSINESS_ID_CLAIM, businessId);
        }
        return builder.build();
    }

    private String sign(SignedJWT jwt) {
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Token could not be signed", exception);
        }
    }

    private static JWSSigner signerFor(RsaKeyProvider keyProvider) {
        try {
            return new RSASSASigner(keyProvider.signingKey());
        } catch (JOSEException exception) {
            throw new IllegalStateException("Token signer could not be created", exception);
        }
    }
}
