package com.geopslabs.geops.identity.application.usecases;

public interface LogOutUseCase {
    /** Revokes the token. Closing an already revoked token again is accepted. */
    void logOut(String token);
}
