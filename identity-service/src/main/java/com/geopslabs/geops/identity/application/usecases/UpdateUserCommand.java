package com.geopslabs.geops.identity.application.usecases;

public record UpdateUserCommand(
    Long id,
    String name,
    String email,
    String phone,
    String role
) {
    public UpdateUserCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}
