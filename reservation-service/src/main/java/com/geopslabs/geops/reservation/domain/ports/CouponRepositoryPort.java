package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepositoryPort {
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAll();

    List<Coupon> findByUserId(Long userId);

    List<Coupon> findByPaymentId(Long paymentId);

    List<Coupon> findByPaymentCode(String paymentCode);

    List<Coupon> findByOfferId(Long offerId);

    List<Coupon> findByProductType(String productType);

    List<Coupon> findValidCouponsByUserId(Long userId, String currentTime);

    List<Coupon> findExpiredCoupons(String currentTime);

    long countByUserId(Long userId);

    boolean existsByCode(String code);

    void deleteById(Long id);
}
