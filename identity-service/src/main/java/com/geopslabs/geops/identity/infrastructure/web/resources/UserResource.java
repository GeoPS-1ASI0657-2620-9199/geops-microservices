package com.geopslabs.geops.identity.infrastructure.web.resources;

public record UserResource(
    Long id,
    String name,
    String email,
    String phone,
    String role,
    java.util.Date createdAt,
    java.util.Date updatedAt
) {
}
