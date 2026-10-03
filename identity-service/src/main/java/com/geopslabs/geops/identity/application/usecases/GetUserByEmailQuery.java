package com.geopslabs.geops.identity.application.usecases;

public record GetUserByEmailQuery(String email) {
    public GetUserByEmailQuery {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
    }
}
