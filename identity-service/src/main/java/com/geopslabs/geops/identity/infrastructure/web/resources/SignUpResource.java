package com.geopslabs.geops.identity.infrastructure.web.resources;

public record SignUpResource(
    String name,
    String email,
    String phone,
    String password,
    String role
) {
}
