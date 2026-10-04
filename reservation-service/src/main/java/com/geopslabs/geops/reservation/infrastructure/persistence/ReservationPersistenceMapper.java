package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {
    }

    static Reservation toDomain(ReservationJpaEntity entity) {
        return new Reservation(entity.getId(), entity.getUser(), entity.getPayment(), entity.getPaymentCode(),
                entity.getProductType(), entity.getOfferId(), entity.getCode(), entity.getExpiresAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static ReservationJpaEntity toEntity(Reservation reservation, ReservationJpaEntity entity) {
        entity.setUser(reservation.getUser());
        entity.setPayment(reservation.getPayment());
        entity.setPaymentCode(reservation.getPaymentCode());
        entity.setProductType(reservation.getProductType());
        entity.setOfferId(reservation.getOfferId());
        entity.setCode(reservation.getCode());
        entity.setExpiresAt(reservation.getExpiresAt());
        return entity;
    }
}
