package com.geopslabs.geops.identity.application.usecases;

public interface RegisterUserUseCase {
    RegisteredUser register(RegisterUserCommand command);
}
