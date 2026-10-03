package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "details_consumer")
@Getter
public class ConsumerProfileJpaEntity extends AuditableAbstractAggregateRoot<ConsumerProfileJpaEntity> {
    private static final int TEXT_LENGTH = 255;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "categorias_favoritas", length = TEXT_LENGTH)
    private String categoriasFavoritas;

    @Column(name = "recibir_notificaciones", nullable = false)
    private Boolean recibirNotificaciones;

    @Column(name = "permiso_ubicacion", nullable = false)
    private Boolean permisoUbicacion;

    @Column(name = "direccion_casa", length = TEXT_LENGTH)
    private String direccionCasa;

    @Column(name = "direccion_trabajo", length = TEXT_LENGTH)
    private String direccionTrabajo;

    @Column(name = "direccion_universidad", length = TEXT_LENGTH)
    private String direccionUniversidad;

    protected ConsumerProfileJpaEntity() {
    }

    public ConsumerProfileJpaEntity(UserJpaEntity user) {
        this.user = user;
    }

    public void update(String categoriasFavoritas, Boolean recibirNotificaciones, Boolean permisoUbicacion,
                       String direccionCasa, String direccionTrabajo, String direccionUniversidad) {
        this.categoriasFavoritas = categoriasFavoritas;
        this.recibirNotificaciones = recibirNotificaciones;
        this.permisoUbicacion = permisoUbicacion;
        this.direccionCasa = direccionCasa;
        this.direccionTrabajo = direccionTrabajo;
        this.direccionUniversidad = direccionUniversidad;
    }
}
