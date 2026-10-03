package com.geopslabs.geops.identity.infrastructure.web.resources;

public record AuthenticationResource(
    Long id,
    String name,
    String email,
    String phone,
    String role,
    String token,
    String message
) {
}
