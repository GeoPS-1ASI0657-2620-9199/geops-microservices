package com.geopslabs.geops.identity.infrastructure.web.resources;

public record DetailsConsumerResource(
    Long id,
    Long userId,
    String categoriasFavoritas,
    Boolean permisoUbicacion,
    String direccionCasa,
    String direccionTrabajo,
    String direccionUniversidad,
    java.util.Date createdAt,
    java.util.Date updatedAt
) {
}
