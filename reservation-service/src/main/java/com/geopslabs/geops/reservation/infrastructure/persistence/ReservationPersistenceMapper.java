package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {
    }

    static Reservation toDomain(ReservationJpaEntity entity) {
        return new Reservation(entity.getId(), entity.getConsumerId(), entity.getOfferId(), entity.getCode(),
                toLocal(entity.getExpiresAt()));
    }

    static ReservationJpaEntity toEntity(Reservation reservation, ReservationJpaEntity entity) {
        entity.setConsumerId(reservation.getConsumerId());
        entity.setOfferId(reservation.getOfferId());
        entity.setCode(reservation.getCode());
        entity.setExpiresAt(toInstant(reservation.getExpiresAt()));
        return entity;
    }

    private static LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
