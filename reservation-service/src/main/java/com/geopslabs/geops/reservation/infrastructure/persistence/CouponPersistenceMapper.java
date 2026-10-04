package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.Coupon;

final class CouponPersistenceMapper {

    private CouponPersistenceMapper() {
    }

    static Coupon toDomain(CouponJpaEntity entity) {
        return new Coupon(entity.getId(), entity.getUser(), entity.getPayment(), entity.getPaymentCode(),
                entity.getProductType(), entity.getOfferId(), entity.getCode(), entity.getExpiresAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static CouponJpaEntity toEntity(Coupon coupon, CouponJpaEntity entity) {
        entity.setUser(coupon.getUser());
        entity.setPayment(coupon.getPayment());
        entity.setPaymentCode(coupon.getPaymentCode());
        entity.setProductType(coupon.getProductType());
        entity.setOfferId(coupon.getOfferId());
        entity.setCode(coupon.getCode());
        entity.setExpiresAt(coupon.getExpiresAt());
        return entity;
    }
}
