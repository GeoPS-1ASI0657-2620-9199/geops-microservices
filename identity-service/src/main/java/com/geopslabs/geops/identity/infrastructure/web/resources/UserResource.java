package com.geopslabs.geops.identity.infrastructure.web.resources;

import java.time.Instant;

public record UserResource(
    Long id,
    String name,
    String email,
    String phone,
    String role,
    Instant createdAt
) {
}
