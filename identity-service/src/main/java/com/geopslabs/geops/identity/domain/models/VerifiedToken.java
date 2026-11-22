package com.geopslabs.geops.identity.domain.models;

import java.time.Instant;

/**
 * What a token signed by Identity says once its signature, issuer, audience and expiry were checked. businessId only
 * comes for a business owner.
 */
public record VerifiedToken(TokenFingerprint fingerprint, Long userId, Role role, Long businessId, Instant issuedAt,
                            Instant expiresAt) {
}
