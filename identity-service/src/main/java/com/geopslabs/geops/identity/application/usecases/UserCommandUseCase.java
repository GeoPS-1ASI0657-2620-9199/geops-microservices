package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.Optional;

public interface UserCommandUseCase {
    Optional<User> handle(CreateUserCommand command);
}
