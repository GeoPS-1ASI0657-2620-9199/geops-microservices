package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.Role;

public record RegisterUserCommand(Role role, String fullName, String email, String phone, String password) {
}
