package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;

public class Reservation {
    private Long id;
    private Long consumerId;
    private Long paymentId;
    private String paymentCode;
    private String productType;
    private Long offerId;
    private String code;
    private String expiresAt;

    public Reservation(CreateReservationCommand command) {
        this.consumerId = command.consumerId();
        this.paymentId = command.paymentId();
        this.paymentCode = command.paymentCode();
        this.productType = command.productType();
        this.offerId = command.offerId();
        this.code = command.code();
        this.expiresAt = command.expiresAt();
    }

    public Reservation(Long id, Long consumerId, Long paymentId, String paymentCode, String productType,
                       Long offerId, String code, String expiresAt) {
        this.id = id;
        this.consumerId = consumerId;
        this.paymentId = paymentId;
        this.paymentCode = paymentCode;
        this.productType = productType;
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

    public Long getPaymentId() {
        return paymentId;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public String getProductType() {
        return productType;
    }

    public Long getOfferId() {
        return offerId;
    }

    public String getCode() {
        return code;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void updateReservation(UpdateReservationCommand command) {
        if (command.productType() != null) {
            this.productType = command.productType();
        }
        if (command.offerId() != null) {
            this.offerId = command.offerId();
        }
        if (command.code() != null) {
            this.code = command.code();
        }
        if (command.expiresAt() != null) {
            this.expiresAt = command.expiresAt();
        }
    }

    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        return this.expiresAt.compareTo(java.time.Instant.now().toString()) < 0;
    }

    public boolean isValid() {
        return !isExpired() && this.code != null && !this.code.isBlank();
    }
}
