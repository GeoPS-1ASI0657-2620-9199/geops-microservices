package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class HmacTokenIssuerAdapter implements TokenIssuerPort {
    private final SecretKey signingKey;
    private final Duration expiration;

    public HmacTokenIssuerAdapter(@Value("${authorization.jwt.secret}") String secret,
                                  @Value("${authorization.jwt.expiration.days}") long expirationDays) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofDays(expirationDays);
    }

    @Override
    public String generateToken(String username) {
        var issuedAt = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }
}
