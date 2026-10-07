package com.geopslabs.geops.identity.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geopslabs.geops.identity.application.usecases.LogInResult;
import com.geopslabs.geops.identity.domain.models.Role;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String accessToken, String tokenType, long expiresIn, Long userId, Role role,
                            Long consumerId, Long businessId, String businessName) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(LogInResult result) {
        return new TokenResponse(result.token().value(), BEARER, result.token().lifetime().toSeconds(),
                result.userId(), result.role(), result.consumerId(), result.businessId(),
                result.businessName());
    }
}
