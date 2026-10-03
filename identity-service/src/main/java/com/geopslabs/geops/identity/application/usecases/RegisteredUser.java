package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.Role;

public record RegisteredUser(Long userId, String fullName, String email, Role role, Long consumerProfileId) {
}
