package com.geopslabs.geops.identity.application.usecases;

public record UpdateDetailsConsumerCommand(
    Long userId,
    String categoriasFavoritas,
    Boolean permisoUbicacion,
    String direccionCasa,
    String direccionTrabajo,
    String direccionUniversidad
) {
    public UpdateDetailsConsumerCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}
