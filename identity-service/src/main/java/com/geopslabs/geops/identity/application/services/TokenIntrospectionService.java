package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.IntrospectTokenUseCase;
import com.geopslabs.geops.identity.application.usecases.LogOutUseCase;
import com.geopslabs.geops.identity.application.usecases.PurgeRevokedTokensUseCase;
import com.geopslabs.geops.identity.application.usecases.TokenIntrospection;
import com.geopslabs.geops.identity.domain.models.InvalidTokenException;
import com.geopslabs.geops.identity.domain.models.RevokedToken;
import com.geopslabs.geops.identity.domain.models.VerifiedToken;
import com.geopslabs.geops.identity.domain.ports.RevokedTokenRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TokenVerifierPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Validates the tokens Identity issued (US26) for the gateway and for any service that asks. The signature and the
 * expiry are checked with Identity's own key; the only table read is revoked_tokens, so a logged-out token is
 * reported inactive although its signature is still good.
 */
@Transactional
public class TokenIntrospectionService implements IntrospectTokenUseCase, LogOutUseCase, PurgeRevokedTokensUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenIntrospectionService.class);

    private final TokenVerifierPort tokenVerifier;
    private final RevokedTokenRepositoryPort revokedTokens;
    private final Clock clock;

    public TokenIntrospectionService(TokenVerifierPort tokenVerifier, RevokedTokenRepositoryPort revokedTokens,
                                     Clock clock) {
        this.tokenVerifier = tokenVerifier;
        this.revokedTokens = revokedTokens;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenIntrospection introspect(String token) {
        return activeToken(token)
                .map(TokenIntrospection::activeFor)
                .orElseGet(TokenIntrospection::inactive);
    }

    @Override
    public void logOut(String token) {
        var verified = verifiedToken(token).orElseThrow(() -> {
            LOGGER.info("auth.logout.rejected reason=invalid-token");
            return new InvalidTokenException();
        });
        if (revokedTokens.isRevoked(verified.fingerprint())) {
            LOGGER.info("auth.logout.repeated userId={}", verified.userId());
            return;
        }
        revokedTokens.save(RevokedToken.of(verified, clock.instant()));
        LOGGER.info("auth.logged-out userId={} expiresAt={}", verified.userId(), verified.expiresAt());
    }

    @Override
    public int purgeExpired() {
        return revokedTokens.deleteExpiredBefore(clock.instant());
    }

    private Optional<VerifiedToken> activeToken(String token) {
        var verified = verifiedToken(token);
        if (verified.isEmpty()) {
            LOGGER.info("auth.token.rejected reason=invalid-token");
            return Optional.empty();
        }
        if (revokedTokens.isRevoked(verified.get().fingerprint())) {
            LOGGER.info("auth.token.rejected reason=revoked userId={}", verified.get().userId());
            return Optional.empty();
        }
        LOGGER.debug("auth.token.introspected userId={} role={}", verified.get().userId(), verified.get().role());
        return verified;
    }

    private Optional<VerifiedToken> verifiedToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenVerifier.verify(token);
    }
}
