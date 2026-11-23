package com.geopslabs.geops.identity.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geopslabs.geops.identity.application.usecases.TokenIntrospection;
import com.geopslabs.geops.identity.domain.models.Role;

import java.time.Instant;

/**
 * An active token says who sent it and until when it is valid; an inactive one only says active=false.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntrospectionResponse(boolean active, Long userId, Role role, Long businessId, Instant issuedAt,
                                    Instant expiresAt) {

    public static IntrospectionResponse from(TokenIntrospection introspection) {
        if (!introspection.active()) {
            return new IntrospectionResponse(false, null, null, null, null, null);
        }
        var token = introspection.token();
        return new IntrospectionResponse(true, token.userId(), token.role(), token.businessId(), token.issuedAt(),
                token.expiresAt());
    }
}
