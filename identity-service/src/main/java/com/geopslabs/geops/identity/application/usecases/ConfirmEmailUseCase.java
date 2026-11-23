package com.geopslabs.geops.identity.application.usecases;

public interface ConfirmEmailUseCase {
    void confirm(String token);
}
