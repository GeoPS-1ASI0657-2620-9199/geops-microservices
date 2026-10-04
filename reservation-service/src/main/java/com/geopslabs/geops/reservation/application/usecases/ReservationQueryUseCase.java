package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByPaymentIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * ReservationQueryService
 *
 * Domain service interface that defines query operations for reservation management.
 * This service handles all read operations following the Command Query Responsibility
 * Segregation (CQRS) pattern, providing various ways to retrieve reservation data.
 *
 * @summary Service interface for handling reservation query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface ReservationQueryUseCase {

    /**
     * Handles the query to retrieve a reservation by its unique identifier.
     *
     * This method processes the query to find a specific reservation using its ID.
     * It's commonly used for reservation validation, redemption, and detailed views.
     *
     * @param query The query containing the reservation ID
     * @return An Optional containing the Reservation if found, empty otherwise
     * @throws IllegalArgumentException if the query contains invalid data
     */
    Optional<Reservation> handle(GetReservationByIdQuery query);

    /**
     * Handles the query to retrieve all reservations for a specific user.
     *
     * This method processes the query to find all reservations belonging to a user.
     * It's useful for displaying user reservation history and reservation management.
     *
     * @param query The query containing the user ID
     * @return A List of Reservation objects for the specified user
     * @throws IllegalArgumentException if the query contains invalid data
     */
    List<Reservation> handle(GetReservationsByConsumerIdQuery query);

    /**
     * Handles the query to retrieve reservations generated from a specific payment.
     *
     * This method processes the query to find reservations associated with a payment.
     * It's useful for tracking reservation generation and payment-reservation relationships.
     *
     * @param query The query containing the payment ID
     * @return A List of Reservation objects generated from the specified payment
     * @throws IllegalArgumentException if the query contains invalid data
     */
    List<Reservation> handle(GetReservationsByPaymentIdQuery query);

    /**
     * Handles the query to retrieve a reservation by its redemption code.
     *
     * This method processes the query to find a reservation using its unique code.
     * It's essential for reservation redemption processes and validation.
     *
     * @param query The query containing the reservation code
     * @return An Optional containing the Reservation if found, empty otherwise
     * @throws IllegalArgumentException if the query contains invalid data
     */
    Optional<Reservation> handle(GetReservationByCodeQuery query);

    /**
     * Retrieves all reservations in the system.
     *
     * This method provides a comprehensive view of all reservations,
     * useful for administrative dashboards and reporting purposes.
     *
     * @return A List of all Reservation objects in the system
     */
    List<Reservation> getAllReservations();

    /**
     * Retrieves all valid (non-expired) reservations for a specific user.
     *
     * This method finds only valid reservations for a user,
     * useful for displaying redeemable reservations in user interfaces.
     *
     * @param consumerId The unique identifier of the user
     * @return A List of valid Reservation objects for the specified user
     * @throws IllegalArgumentException if consumerId is null or empty
     */
    List<Reservation> getValidReservationsByConsumerId(Long consumerId);

    /**
     * Retrieves expired reservations for cleanup or analysis purposes.
     *
     * This method finds reservations that have passed their expiration date,
     * useful for cleanup operations and expired reservation management.
     *
     * @return A List of expired Reservation objects
     */
    List<Reservation> getExpiredReservations();
}
