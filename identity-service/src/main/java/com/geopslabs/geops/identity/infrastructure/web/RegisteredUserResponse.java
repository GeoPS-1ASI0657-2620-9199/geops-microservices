package com.geopslabs.geops.identity.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geopslabs.geops.identity.application.usecases.RegisteredUser;
import com.geopslabs.geops.identity.domain.models.AccountStatus;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisteredUserResponse(Long userId, String fullName, String email, Role role, Long consumerProfileId,
                                     Long businessProfileId, AccountStatus accountStatus,
                                     VerificationStatus verificationStatus) {

    public static RegisteredUserResponse from(RegisteredUser user) {
        return new RegisteredUserResponse(user.userId(), user.fullName(), user.email(), user.role(),
                user.consumerProfileId(), user.businessProfileId(), user.accountStatus(),
                user.verificationStatus());
    }
}
