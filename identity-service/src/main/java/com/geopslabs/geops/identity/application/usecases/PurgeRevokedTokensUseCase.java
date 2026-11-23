package com.geopslabs.geops.identity.application.usecases;

public interface PurgeRevokedTokensUseCase {
    /** Deletes the revocations of tokens that already expired; returns how many. */
    int purgeExpired();
}
