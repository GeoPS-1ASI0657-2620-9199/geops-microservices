package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ReservationPersistenceMapper {

    private ReservationPersistenceMapper() {
    }

    static Reservation toDomain(ReservationJpaEntity entity) {
        return new Reservation(entity.getId(), new ReservationCode(entity.getCode()), entity.getConsumerId(),
                entity.getOfferId(), entity.getBusinessId(), entity.getOfferTitle(), toUtc(entity.getReservedAt()),
                toUtc(entity.getExpiresAt()), toUtc(entity.getRedeemedAt()), entity.getStatus());
    }

    static ReservationJpaEntity toEntity(Reservation reservation) {
        var entity = new ReservationJpaEntity();
        entity.setId(reservation.getId());
        entity.setCode(reservation.getCode().value());
        entity.setConsumerId(reservation.getConsumerId());
        entity.setOfferId(reservation.getOfferId());
        entity.setBusinessId(reservation.getBusinessId());
        entity.setOfferTitle(reservation.getOfferTitle());
        entity.setReservedAt(toInstant(reservation.getReservedAt()));
        entity.setExpiresAt(toInstant(reservation.getExpiresAt()));
        entity.setRedeemedAt(toInstant(reservation.getRedeemedAt()));
        entity.setStatus(reservation.getStatus());
        return entity;
    }

    private static LocalDateTime toUtc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
