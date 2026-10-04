package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByPaymentIdQuery;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ReservationQueryService
 *
 * Implementation of the ReservationQueryUseCase that handles all query operations
 * for reservation management. This service implements the business logic for
 * retrieving and searching reservations following DDD principles.
 *
 * @summary Implementation of reservation query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional(readOnly = true)
public class ReservationQueryService implements ReservationQueryUseCase {

    private final ReservationRepositoryPort reservationRepository;

    /**
     * Constructor for dependency injection
     *
     * @param reservationRepository The repository for reservation data access
     */
    public ReservationQueryService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Reservation> handle(GetReservationByIdQuery query) {
        try {
            return reservationRepository.findById(query.reservationId());
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservation by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> handle(GetReservationsByConsumerIdQuery query) {
        try {
            return reservationRepository.findByUserId(Long.valueOf(query.userId()));
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservations by user ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> handle(GetReservationsByPaymentIdQuery query) {
        try {
            return reservationRepository.findByPaymentId(query.paymentId());
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservations by payment ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Reservation> handle(GetReservationByCodeQuery query) {
        try {
            return reservationRepository.findByCode(query.code());
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservation by code: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> getAllReservations() {
        try {
            return reservationRepository.findAll();
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving all reservations: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> getValidReservationsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        try {
            String currentTime = Instant.now().toString();
            return reservationRepository.findValidReservationsByUserId(userId, currentTime);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving valid reservations by user ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> getExpiredReservations() {
        try {
            String currentTime = Instant.now().toString();
            return reservationRepository.findExpiredReservations(currentTime);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving expired reservations: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves reservations by payment code.
     *
     * This method provides a way to get reservations generated from a specific payment code,
     * which can be useful for tracking and validation purposes.
     *
     * @param paymentCode The payment code used to generate reservations
     * @return A List of Reservation objects with the specified payment code
     * @throws IllegalArgumentException if paymentCode is null or empty
     */
    public List<Reservation> getReservationsByPaymentCode(String paymentCode) {
        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException("paymentCode cannot be null or empty");
        }

        try {
            return reservationRepository.findByPaymentCode(paymentCode);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservations by payment code: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves reservations by offer ID.
     *
     * This method finds reservations associated with a specific offer,
     * useful for offer-based reservation management and analytics.
     *
     * @param offerId The unique identifier of the offer
     * @return A List of Reservation objects associated with the specified offer
     * @throws IllegalArgumentException if offerId is null or negative
     */
    public List<Reservation> getReservationsByOfferId(Long offerId) {
        if (offerId == null || offerId <= 0) {
            throw new IllegalArgumentException("offerId cannot be null or negative");
        }

        try {
            return reservationRepository.findByOfferId(offerId);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservations by offer ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves reservations by product type.
     *
     * This method finds reservations associated with a specific product type,
     * useful for product-based reservation filtering and management.
     *
     * @param productType The product type to filter by
     * @return A List of Reservation objects with the specified product type
     * @throws IllegalArgumentException if productType is null or empty
     */
    public List<Reservation> getReservationsByProductType(String productType) {
        if (productType == null || productType.isBlank()) {
            throw new IllegalArgumentException("productType cannot be null or empty");
        }

        try {
            return reservationRepository.findByProductType(productType);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error retrieving reservations by product type: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Counts reservations for a specific user.
     *
     * This method provides a quick count of reservations belonging to a user,
     * useful for analytics and user interface purposes.
     *
     * @param userId The unique identifier of the user
     * @return The count of reservations for the specified user
     * @throws IllegalArgumentException if userId is null or empty
     */
    public long getReservationCountByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        try {
            return reservationRepository.countByUserId(userId);
        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error counting reservations by user ID: " + e.getMessage());
            return 0;
        }
    }
}
