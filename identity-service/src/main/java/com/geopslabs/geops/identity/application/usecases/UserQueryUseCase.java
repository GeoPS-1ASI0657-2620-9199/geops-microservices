package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.Optional;

public interface UserQueryUseCase {
    Optional<User> handle(GetUserByEmailQuery query);

    Optional<User> handle(GetUserByPhoneQuery query);
}
