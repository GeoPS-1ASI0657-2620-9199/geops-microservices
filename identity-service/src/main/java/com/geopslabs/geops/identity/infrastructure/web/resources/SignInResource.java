package com.geopslabs.geops.identity.infrastructure.web.resources;

public record SignInResource(
    String email,
    String password
) {
}
