package com.geopslabs.geops.identity.domain.models;

import java.util.Date;

public class ConsumerProfile {
    private Long id;
    private User user;
    private String categoriasFavoritas;
    private Boolean permisoUbicacion;
    private String direccionCasa;
    private String direccionTrabajo;
    private String direccionUniversidad;
    private Date createdAt;
    private Date updatedAt;

    public ConsumerProfile(User user, String categoriasFavoritas, Boolean permisoUbicacion, String direccionCasa,
                           String direccionTrabajo, String direccionUniversidad) {
        this.user = user;
        this.categoriasFavoritas = categoriasFavoritas;
        this.permisoUbicacion = Boolean.TRUE.equals(permisoUbicacion);
        this.direccionCasa = direccionCasa;
        this.direccionTrabajo = direccionTrabajo;
        this.direccionUniversidad = direccionUniversidad;
    }

    public ConsumerProfile(Long id, ConsumerProfile data, Date createdAt, Date updatedAt) {
        this(data.user, data.categoriasFavoritas, data.permisoUbicacion, data.direccionCasa,
                data.direccionTrabajo, data.direccionUniversidad);
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateConsumerDetails(String categoriasFavoritas, Boolean permisoUbicacion, String direccionCasa,
                                      String direccionTrabajo, String direccionUniversidad) {
        this.categoriasFavoritas = valueOrCurrent(categoriasFavoritas, this.categoriasFavoritas);
        this.permisoUbicacion = valueOrCurrent(permisoUbicacion, this.permisoUbicacion);
        this.direccionCasa = valueOrCurrent(direccionCasa, this.direccionCasa);
        this.direccionTrabajo = valueOrCurrent(direccionTrabajo, this.direccionTrabajo);
        this.direccionUniversidad = valueOrCurrent(direccionUniversidad, this.direccionUniversidad);
    }

    private static <T> T valueOrCurrent(T candidate, T current) {
        return candidate != null ? candidate : current;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCategoriasFavoritas() {
        return categoriasFavoritas;
    }

    public Boolean getPermisoUbicacion() {
        return permisoUbicacion;
    }

    public String getDireccionCasa() {
        return direccionCasa;
    }

    public String getDireccionTrabajo() {
        return direccionTrabajo;
    }

    public String getDireccionUniversidad() {
        return direccionUniversidad;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
