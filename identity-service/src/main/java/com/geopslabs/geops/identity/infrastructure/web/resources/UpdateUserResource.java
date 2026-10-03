package com.geopslabs.geops.identity.infrastructure.web.resources;

public record UpdateUserResource(
    String name,
    String email,
    String phone,
    String role
) {
}
