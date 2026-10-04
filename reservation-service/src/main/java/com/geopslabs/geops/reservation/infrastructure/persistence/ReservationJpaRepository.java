package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ReservationJpaRepository
 *
 * JPA Repository interface for Reservation aggregate root.
 * This repository provides data access operations for reservation management,
 * including custom queries for user reservations, payment reservations, and validation.
 *
 * @summary JPA Repository for reservation data access operations
 * @since 1.0
 * @author GeOps Labs
 */
@Repository
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    /**
     * Finds all reservations for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return A List of Reservation objects for the specified user
     */
    List<ReservationJpaEntity> findByUser_Id(Long userId);

    /**
     * Finds all reservations generated from a specific payment.
     *
     * @param paymentId The unique identifier of the payment
     * @return A List of Reservation objects generated from the specified payment
     */
    List<ReservationJpaEntity> findByPayment_Id(Long paymentId);

    /**
     * Finds a reservation by its unique redemption code.
     *
     * @param code The reservation redemption code
     * @return An Optional containing the Reservation if found, empty otherwise
     */
    Optional<ReservationJpaEntity> findByCode(String code);

    /**
     * Finds reservations by payment code.
     *
     * @param paymentCode The payment code used to generate reservations
     * @return A List of Reservation objects with the specified payment code
     */
    List<ReservationJpaEntity> findByPaymentCode(String paymentCode);

    /**
     * Finds reservations by offer ID.
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Reservation objects associated with the specified offer
     */
    List<ReservationJpaEntity> findByOfferId(Long offerId);

    /**
     * Finds valid (non-expired) reservations for a specific user.
     *
     * This query finds reservations where the expiration date is null (no expiration)
     * or the expiration date is in the future.
     *
     * @param userId The unique identifier of the user
     * @param currentTime The current timestamp for comparison
     * @return A List of valid Reservation objects for the specified user
     */
    @Query("SELECT c FROM ReservationJpaEntity c WHERE c.user.id = :userId AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :currentTime)")
    List<ReservationJpaEntity> findValidReservationsByUserId(@Param("userId") Long userId,
                                         @Param("currentTime") String currentTime);

    /**
     * Finds expired reservations.
     *
     * This query finds reservations that have passed their expiration date.
     *
     * @param currentTime The current timestamp for comparison
     * @return A List of expired Reservation objects
     */
    @Query("SELECT c FROM ReservationJpaEntity c WHERE c.expiresAt IS NOT NULL AND c.expiresAt <= :currentTime")
    List<ReservationJpaEntity> findExpiredReservations(@Param("currentTime") String currentTime);

    /**
     * Checks if a reservation code already exists.
     *
     * @param code The reservation code to check
     * @return true if a reservation with this code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Finds reservations by product type.
     *
     * @param productType The product type to filter by
     * @return A List of Reservation objects with the specified product type
     */
    List<ReservationJpaEntity> findByProductType(String productType);

    /**
     * Counts reservations for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return The count of reservations for the specified user
     */
    long countByUser_Id(Long userId);

    /**
     * Finds reservations expiring within a specific timeframe.
     *
     * This query is useful for sending expiration notifications.
     *
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return A List of Reservation objects expiring within the specified timeframe
     */
    @Query("SELECT c FROM ReservationJpaEntity c WHERE c.expiresAt BETWEEN :startTime AND :endTime")
    List<ReservationJpaEntity> findReservationsExpiringBetween(@Param("startTime") String startTime,
                                           @Param("endTime") String endTime);
}
