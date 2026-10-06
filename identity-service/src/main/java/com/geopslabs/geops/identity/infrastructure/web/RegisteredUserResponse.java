package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.RegisteredUser;
import com.geopslabs.geops.identity.domain.models.Role;

public record RegisteredUserResponse(Long userId, String fullName, String email, Role role, Long consumerProfileId) {

    public static RegisteredUserResponse from(RegisteredUser user) {
        return new RegisteredUserResponse(user.userId(), user.fullName(), user.email(), user.role(),
                user.consumerProfileId());
    }
}
