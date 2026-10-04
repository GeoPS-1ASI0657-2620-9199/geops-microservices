package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {
    }

    static Reservation toDomain(ReservationJpaEntity entity) {
        return new Reservation(entity.getId(), entity.getConsumerId(), entity.getPaymentId(), entity.getPaymentCode(),
                entity.getProductType(), entity.getOfferId(), entity.getCode(), entity.getExpiresAt());
    }

    static ReservationJpaEntity toEntity(Reservation reservation, ReservationJpaEntity entity) {
        entity.setConsumerId(reservation.getConsumerId());
        entity.setPaymentId(reservation.getPaymentId());
        entity.setPaymentCode(reservation.getPaymentCode());
        entity.setProductType(reservation.getProductType());
        entity.setOfferId(reservation.getOfferId());
        entity.setCode(reservation.getCode());
        entity.setExpiresAt(reservation.getExpiresAt());
        return entity;
    }
}
