package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.AccountStatus;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

public record RegisteredUser(Long userId, String fullName, String email, Role role, Long consumerProfileId,
                             Long businessProfileId, AccountStatus accountStatus,
                             VerificationStatus verificationStatus) {

    public static RegisteredUser consumer(User user, ConsumerProfile profile) {
        return new RegisteredUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                profile.getId(), null, null, null);
    }

    public static RegisteredUser businessOwner(User user, BusinessProfile profile) {
        return new RegisteredUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), null,
                profile.getId(), profile.getAccountStatus(), profile.getVerificationStatus());
    }
}
