package com.geopslabs.geops.identity.infrastructure.scheduling;

import com.geopslabs.geops.identity.application.usecases.PurgeRevokedTokensUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every hour deletes the revocations of tokens that already expired: an expired token is rejected anyway, so
 * revoked_tokens only holds the sessions closed within the last token lifetime.
 */
@Component
@ConditionalOnProperty(name = "geops.security.revoked-tokens.purge-enabled", havingValue = "true",
        matchIfMissing = true)
public class RevokedTokenPurger {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevokedTokenPurger.class);

    private final PurgeRevokedTokensUseCase purgeRevokedTokens;

    public RevokedTokenPurger(PurgeRevokedTokensUseCase purgeRevokedTokens) {
        this.purgeRevokedTokens = purgeRevokedTokens;
    }

    @Scheduled(initialDelayString = "${geops.security.revoked-tokens.purge-delay:PT1H}",
            fixedDelayString = "${geops.security.revoked-tokens.purge-delay:PT1H}")
    public void purgeExpired() {
        var deleted = purgeRevokedTokens.purgeExpired();
        if (deleted > 0) {
            LOGGER.info("auth.revoked-tokens.purged deleted={}", deleted);
        }
    }
}
