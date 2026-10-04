package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CouponJpaRepository
 *
 * JPA Repository interface for Coupon aggregate root.
 * This repository provides data access operations for coupon management,
 * including custom queries for user coupons, payment coupons, and validation.
 *
 * @summary JPA Repository for coupon data access operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, Long> {

    /**
     * Finds all coupons for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return A List of Coupon objects for the specified user
     */
    List<CouponJpaEntity> findByUser_Id(Long userId);

    /**
     * Finds all coupons generated from a specific payment.
     *
     * @param paymentId The unique identifier of the payment
     * @return A List of Coupon objects generated from the specified payment
     */
    List<CouponJpaEntity> findByPayment_Id(Long paymentId);

    /**
     * Finds a coupon by its unique redemption code.
     *
     * @param code The coupon redemption code
     * @return An Optional containing the Coupon if found, empty otherwise
     */
    Optional<CouponJpaEntity> findByCode(String code);

    /**
     * Finds coupons by payment code.
     *
     * @param paymentCode The payment code used to generate coupons
     * @return A List of Coupon objects with the specified payment code
     */
    List<CouponJpaEntity> findByPaymentCode(String paymentCode);

    /**
     * Finds coupons by offer ID.
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Coupon objects associated with the specified offer
     */
    List<CouponJpaEntity> findByOfferId(Long offerId);

    /**
     * Finds valid (non-expired) coupons for a specific user.
     *
     * This query finds coupons where the expiration date is null (no expiration)
     * or the expiration date is in the future.
     *
     * @param userId The unique identifier of the user
     * @param currentTime The current timestamp for comparison
     * @return A List of valid Coupon objects for the specified user
     */
    @Query("SELECT c FROM CouponJpaEntity c WHERE c.user.id = :userId AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :currentTime)")
    List<CouponJpaEntity> findValidCouponsByUserId(@Param("userId") Long userId,
                                         @Param("currentTime") String currentTime);

    /**
     * Finds expired coupons.
     *
     * This query finds coupons that have passed their expiration date.
     *
     * @param currentTime The current timestamp for comparison
     * @return A List of expired Coupon objects
     */
    @Query("SELECT c FROM CouponJpaEntity c WHERE c.expiresAt IS NOT NULL AND c.expiresAt <= :currentTime")
    List<CouponJpaEntity> findExpiredCoupons(@Param("currentTime") String currentTime);

    /**
     * Checks if a coupon code already exists.
     *
     * @param code The coupon code to check
     * @return true if a coupon with this code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Finds coupons by product type.
     *
     * @param productType The product type to filter by
     * @return A List of Coupon objects with the specified product type
     */
    List<CouponJpaEntity> findByProductType(String productType);

    /**
     * Counts coupons for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return The count of coupons for the specified user
     */
    long countByUser_Id(Long userId);

    /**
     * Finds coupons expiring within a specific timeframe.
     *
     * This query is useful for sending expiration notifications.
     *
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return A List of Coupon objects expiring within the specified timeframe
     */
    @Query("SELECT c FROM CouponJpaEntity c WHERE c.expiresAt BETWEEN :startTime AND :endTime")
    List<CouponJpaEntity> findCouponsExpiringBetween(@Param("startTime") String startTime,
                                           @Param("endTime") String endTime);
}
