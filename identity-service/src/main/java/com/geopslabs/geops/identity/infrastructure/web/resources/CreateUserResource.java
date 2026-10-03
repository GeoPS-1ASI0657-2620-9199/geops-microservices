package com.geopslabs.geops.identity.infrastructure.web.resources;

public record CreateUserResource(
    String name,
    String email,
    String phone,
    String password,
    String role
) {
}
