package com.geopslabs.geops.identity.infrastructure.web.resources;

public record CreateDetailsConsumerResource(
    String categoriasFavoritas,
    Boolean permisoUbicacion,
    String direccionCasa,
    String direccionTrabajo,
    String direccionUniversidad
) {
}
