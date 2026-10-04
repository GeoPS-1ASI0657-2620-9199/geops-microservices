package com.geopslabs.geops.reservation.domain.models;

import java.time.LocalDateTime;

public class Reservation {
    private final Long id;
    private final ReservationCode code;
    private final Long consumerId;
    private final Long offerId;
    private final Long businessId;
    private final String offerTitle;
    private final LocalDateTime reservedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime redeemedAt;
    private final ReservationStatus status;

    @SuppressWarnings("java:S107")
    public Reservation(Long id, ReservationCode code, Long consumerId, Long offerId, Long businessId,
                       String offerTitle, LocalDateTime reservedAt, LocalDateTime expiresAt,
                       LocalDateTime redeemedAt, ReservationStatus status) {
        this.id = id;
        this.code = code;
        this.consumerId = consumerId;
        this.offerId = offerId;
        this.businessId = businessId;
        this.offerTitle = offerTitle;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.redeemedAt = redeemedAt;
        this.status = status;
    }

    public static Reservation create(OfferSnapshot offer, Long consumerId, ReservationCode code,
                                     LocalDateTime reservedAt, LocalDateTime expiresAt) {
        return new Reservation(null, code, consumerId, offer.offerId(), offer.businessId(), offer.title(),
                reservedAt, expiresAt, null, ReservationStatus.ACTIVE);
    }

    public Long getId() {
        return id;
    }

    public ReservationCode getCode() {
        return code;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getOfferTitle() {
        return offerTitle;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
