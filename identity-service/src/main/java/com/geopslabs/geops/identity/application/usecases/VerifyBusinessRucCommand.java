package com.geopslabs.geops.identity.application.usecases;

/**
 * The owner asks to verify business {@code businessId}. {@code requesterBusinessId} comes from the token; a
 * non-null {@code correctedAddress} replaces the declared address before the check.
 */
public record VerifyBusinessRucCommand(Long businessId, Long requesterBusinessId, String correctedAddress) {
}
