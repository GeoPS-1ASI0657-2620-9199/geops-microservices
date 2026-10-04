package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.commands.CreateCouponCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateCouponCommand;
import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.payments.domain.model.aggregates.Payment;

import java.util.Date;

public class Coupon {
    private Long id;
    private User user;
    private Payment payment;
    private String paymentCode;
    private String productType;
    private Long offerId;
    private String code;
    private String expiresAt;
    private Date createdAt;
    private Date updatedAt;

    public Coupon(CreateCouponCommand command, User user, Payment payment) {
        this.user = user;
        this.payment = payment;
        this.paymentCode = command.paymentCode();
        this.productType = command.productType();
        this.offerId = command.offerId();
        this.code = command.code();
        this.expiresAt = command.expiresAt();
    }

    public Coupon(Long id, User user, Payment payment, String paymentCode, String productType, Long offerId,
                  String code, String expiresAt, Date createdAt, Date updatedAt) {
        this.id = id;
        this.user = user;
        this.payment = payment;
        this.paymentCode = paymentCode;
        this.productType = productType;
        this.offerId = offerId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Payment getPayment() {
        return payment;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }

    public Long getPaymentId() {
        return this.payment != null ? this.payment.getId() : null;
    }

    public void updateCoupon(UpdateCouponCommand command) {
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
