package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAlreadyRedeemedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationExpiredException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationReportedException;
import com.geopslabs.geops.reservation.shared.domain.DomainException;

import java.time.LocalDateTime;
import java.util.Optional;

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

    public boolean isOwnedByConsumer(Long candidateConsumerId) {
        return consumerId.equals(candidateConsumerId);
    }

    public boolean isOfBusiness(Long candidateBusinessId) {
        return businessId.equals(candidateBusinessId);
    }

    /**
     * The rules of the conditional update, in the order the rejection is explained: another business,
     * already redeemed, reported, past its deadline. The deadline is exclusive, as in {@code expires_at > now}.
     */
    public Optional<RedeemRejection> rejectionFor(Long candidateBusinessId, LocalDateTime now) {
        if (!isOfBusiness(candidateBusinessId)) {
            return Optional.of(RedeemRejection.OTHER_BUSINESS);
        }
        if (status == ReservationStatus.REDEEMED) {
            return Optional.of(RedeemRejection.ALREADY_REDEEMED);
        }
        if (status == ReservationStatus.REPORTED) {
            return Optional.of(RedeemRejection.REPORTED);
        }
        if (status == ReservationStatus.EXPIRED || !expiresAt.isAfter(now)) {
            return Optional.of(RedeemRejection.EXPIRED);
        }
        return Optional.empty();
    }

    /**
     * Returns the redeemed reservation or throws the exception of the first rule it breaks. The stored
     * transition is the conditional update of the repository; this method states the same rules.
     */
    public Reservation redeem(Long candidateBusinessId, LocalDateTime now) {
        var rejection = rejectionFor(candidateBusinessId, now);
        if (rejection.isPresent()) {
            throw rejectionException(rejection.get());
        }
        return new Reservation(id, code, consumerId, offerId, businessId, offerTitle, reservedAt, expiresAt, now,
                ReservationStatus.REDEEMED);
    }

    public DomainException rejectionException(RedeemRejection rejection) {
        return switch (rejection) {
            case OTHER_BUSINESS -> new ReservationAccessDeniedException(id);
            case ALREADY_REDEEMED -> new ReservationAlreadyRedeemedException(id, redeemedAt);
            case REPORTED -> new ReservationReportedException(id);
            case EXPIRED -> new ReservationExpiredException(id);
            case NOT_FOUND -> throw new IllegalArgumentException("A loaded reservation always exists");
        };
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
