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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class RsaTokenIssuerAdapter implements TokenIssuerPort {
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);
    private static final String ROLE_CLAIM = "role";
    private static final Map<Role, String> PROFILE_CLAIMS =
            Map.of(Role.CONSUMER, "consumerId", Role.BUSINESS_OWNER, "businessId");

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
    public IssuedToken issue(User user, Long profileId) {
        var claims = claimsFor(user, profileId);
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

    private JWTClaimsSet claimsFor(User user, Long profileId) {
        var issuedAt = clock.instant();
        var builder = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .audience(properties.audience())
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole().name())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(TOKEN_LIFETIME)))
                .jwtID(UUID.randomUUID().toString());
        Optional.ofNullable(PROFILE_CLAIMS.get(user.getRole()))
                .ifPresent(claimName -> builder.claim(claimName, profileId));
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
