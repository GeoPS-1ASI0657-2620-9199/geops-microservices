package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Reservation {
    private Long id;
    private Long consumerId;
    private Long offerId;
    private String code;
    private LocalDateTime expiresAt;

    public Reservation(CreateReservationCommand command) {
        this.consumerId = command.consumerId();
        this.offerId = command.offerId();
        this.code = command.code();
        this.expiresAt = toUtc(command.expiresAt());
    }

    public Reservation(Long id, Long consumerId, Long offerId, String code, LocalDateTime expiresAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.offerId = offerId;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void updateReservation(UpdateReservationCommand command) {
        if (command.offerId() != null) {
            this.offerId = command.offerId();
        }
        if (command.code() != null) {
            this.code = command.code();
        }
        if (command.expiresAt() != null) {
            this.expiresAt = toUtc(command.expiresAt());
        }
    }

    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        return this.expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    public boolean isValid() {
        return !isExpired() && this.code != null && !this.code.isBlank();
    }

    private static LocalDateTime toUtc(String instant) {
        return instant == null ? null : LocalDateTime.ofInstant(Instant.parse(instant), ZoneOffset.UTC);
    }
}
