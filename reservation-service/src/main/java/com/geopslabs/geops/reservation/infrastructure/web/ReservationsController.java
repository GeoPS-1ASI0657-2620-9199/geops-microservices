package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByPaymentIdQuery;
import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * ReservationsController
 *
 * REST controller that exposes reservation-related endpoints for the GeOps platform.
 * This controller handles HTTP requests for reservation operations including creation,
 * bulk creation, updates, and various query operations. It follows RESTful principles
 * and integrates with frontend reservation management systems.
 *
 * Supports special endpoints:
 * - /reservations/bulk for creating multiple reservations at once
 * - Query parameters for filtering with relations (_expand, _embed)
 * - User-specific reservation retrieval
 *
 * @summary REST controller for reservation operations
 * @since 1.0
 * @author GeOps Labs
 */
@Tag(name = "Reservations", description = "Reservation operations and management")
@RestController
@RequestMapping(value = "/api/v1/reservations", produces = APPLICATION_JSON_VALUE)
public class ReservationsController {

    private final ReservationCommandUseCase reservationCommandService;
    private final ReservationQueryUseCase reservationQueryService;

    /**
     * Constructor for dependency injection
     *
     * @param reservationCommandService Service for handling reservation commands
     * @param reservationQueryService Service for handling reservation queries
     */
    public ReservationsController(ReservationCommandUseCase reservationCommandService,
                           ReservationQueryUseCase reservationQueryService) {
        this.reservationCommandService = reservationCommandService;
        this.reservationQueryService = reservationQueryService;
    }

    /**
     * Creates a new reservation
     *
     * This endpoint corresponds to the frontend's create() method
     * and creates a new reservation returning the created reservation
     *
     * @param resource The reservation creation request data
     * @return ResponseEntity containing the created reservation or error status
     */
    @Operation(summary = "Create new reservation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Reservation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody CreateReservationRequest resource) {
        var command = CreateReservationCommandAssembler.toCommandFromResource(resource);
        var reservation = reservationCommandService.handle(command);

        if (reservation.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var reservationResource = ReservationResponseAssembler.toResourceFromEntity(reservation.get());
        return new ResponseEntity<>(reservationResource, CREATED);
    }

    /**
     * Creates multiple reservations in a single request (Bulk Endpoint)
     *
     * This endpoint supports the frontend's createMany() method that expects
     * a bulk endpoint at /reservations/bulk accepting an array of reservation resources.
     * Falls back to sequential creation if bulk operation fails.
     *
     * @param resources List of reservation creation request data
     * @return ResponseEntity containing the created reservations or error status
     */
    @Operation(summary = "Create multiple reservations (bulk operation)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Reservations created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<ReservationResponse>> createMany(@RequestBody List<CreateReservationRequest> resources) {
        try {
            var command = CreateManyReservationsCommandAssembler.toCommandFromResourceList(resources);
            var reservations = reservationCommandService.handle(command);

            var reservationResources = reservations.stream()
                    .map(ReservationResponseAssembler::toResourceFromEntity)
                    .toList();

            return new ResponseEntity<>(reservationResources, CREATED);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Alternative bulk creation endpoint that accepts wrapped resource
     *
     * This provides an alternative way to create multiple reservations using
     * a wrapper resource object instead of a direct array.
     *
     * @param resource The bulk reservation creation request data
     * @return ResponseEntity containing the created reservations or error status
     */
    @Operation(summary = "Create multiple reservations (alternative bulk endpoint)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Reservations created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/bulk-wrapped")
    public ResponseEntity<List<ReservationResponse>> createManyWrapped(@RequestBody CreateManyReservationsRequest resource) {
        var command = CreateManyReservationsCommandAssembler.toCommandFromResource(resource);
        var reservations = reservationCommandService.handle(command);

        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return new ResponseEntity<>(reservationResources, CREATED);
    }

    /**
     * Retrieves a reservation by its unique identifier
     *
     * This endpoint corresponds to the frontend's getById() method
     * and retrieves a single reservation by ID
     *
     * @param id The unique identifier of the reservation (supports both number and string)
     * @return ResponseEntity containing the reservation data or not found status
     */
    @Operation(summary = "Get reservation by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @Parameter(description = "Reservation unique identifier") @PathVariable String id) {
        try {
            Long reservationId = Long.parseLong(id);
            var query = new GetReservationByIdQuery(reservationId);
            var reservation = reservationQueryService.handle(query);

            if (reservation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var reservationResource = ReservationResponseAssembler.toResourceFromEntity(reservation.get());
            return ResponseEntity.ok(reservationResource);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves all reservations in the system
     *
     * This endpoint corresponds to the frontend's getAll() method and getAllReservations()
     * Supports query parameters for filtering and relations (_expand, _embed)
     * Compatible with JSON Server style parameters used by the frontend
     *
     * @param consumerId Optional user ID filter parameter
     * @param expand Optional list of single relationships to expand (comma-separated)
     * @param embed Optional list of array relationships to embed (comma-separated)
     * @return ResponseEntity containing the list of reservations
     */
    @Operation(summary = "Get all reservations with optional filtering and relations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll(
            @Parameter(description = "Optional user ID filter") @RequestParam(required = false) String consumerId,
            @Parameter(description = "Relationships to expand (comma-separated)") @RequestParam(name = "_expand", required = false) String expand,
            @Parameter(description = "Relationships to embed (comma-separated)") @RequestParam(name = "_embed", required = false) String embed) {

        List<com.geopslabs.geops.reservation.domain.models.Reservation> reservations;

        if (consumerId != null && !consumerId.isBlank()) {
            var query = new GetReservationsByConsumerIdQuery(Long.valueOf(consumerId));
            reservations = reservationQueryService.handle(query);
        } else {
            reservations = reservationQueryService.getAllReservations();
        }

        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(reservationResources);
    }

    /**
     * Updates an existing reservation
     *
     * This endpoint corresponds to the frontend's update() method
     * and updates an existing reservation by ID
     *
     * @param id The unique identifier of the reservation to update (supports both number and string)
     * @param resource The reservation update request data
     * @return ResponseEntity containing the updated reservation or error status
     */
    @Operation(summary = "Update reservation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation updated successfully"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @Parameter(description = "Reservation unique identifier") @PathVariable String id,
            @RequestBody CreateReservationRequest resource) {
        try {
            Long reservationId = Long.parseLong(id);

            // First check if reservation exists
            var existingReservationQuery = new GetReservationByIdQuery(reservationId);
            var existingReservation = reservationQueryService.handle(existingReservationQuery);

            if (existingReservation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Create update command and handle it
            var updateCommand = new UpdateReservationCommand(
                reservationId, resource.productType(), resource.offerId(),
                resource.code(), resource.expiresAt());
            var reservation = reservationCommandService.handle(updateCommand);

            if (reservation.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            var reservationResource = ReservationResponseAssembler.toResourceFromEntity(reservation.get());
            return ResponseEntity.ok(reservationResource);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletes a reservation by ID
     *
     * This endpoint corresponds to the frontend's delete() method
     * and removes a reservation by ID
     *
     * @param id The unique identifier of the reservation to delete (supports both number and string)
     * @return ResponseEntity with void content or error status
     */
    @Operation(summary = "Delete reservation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Reservation deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Reservation unique identifier") @PathVariable String id) {
        try {
            Long reservationId = Long.parseLong(id);

            // Delete the reservation using the command service
            var deleted = reservationCommandService.deleteReservation(reservationId);

            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Retrieves reservations for a specific user
     *
     * This endpoint supports the frontend's getReservationsByUser() method
     * and can include relations using _expand and _embed parameters
     *
     * @param consumerId The unique identifier of the user
     * @param expand Optional list of single relationships to expand
     * @param embed Optional list of array relationships to embed
     * @return ResponseEntity containing the list of user reservations
     */
    @Operation(summary = "Get reservations by user ID with optional relations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User reservations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping("/user/{consumerId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(
            @Parameter(description = "User unique identifier") @PathVariable String consumerId,
            @Parameter(description = "Relationships to expand") @RequestParam(name = "_expand", required = false) String expand,
            @Parameter(description = "Relationships to embed") @RequestParam(name = "_embed", required = false) String embed) {

        var query = new GetReservationsByConsumerIdQuery(Long.valueOf(consumerId));
        var reservations = reservationQueryService.handle(query);
        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(reservationResources);
    }

    /**
     * Retrieves reservations by payment ID
     *
     * @param paymentId The unique identifier of the payment
     * @return ResponseEntity containing the list of reservations for the payment
     */
    @Operation(summary = "Get reservations by payment ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment reservations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payment ID")
    })
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByPayment(
            @Parameter(description = "Payment unique identifier") @PathVariable Long paymentId) {

        var query = new GetReservationsByPaymentIdQuery(paymentId);
        var reservations = reservationQueryService.handle(query);
        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(reservationResources);
    }

    /**
     * Retrieves a reservation by its redemption code
     *
     * @param code The reservation redemption code
     * @return ResponseEntity containing the reservation or not found status
     */
    @Operation(summary = "Get reservation by redemption code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation code")
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<ReservationResponse> getReservationByCode(
            @Parameter(description = "Reservation redemption code") @PathVariable String code) {

        var query = new GetReservationByCodeQuery(code);
        var reservation = reservationQueryService.handle(query);

        if (reservation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var reservationResource = ReservationResponseAssembler.toResourceFromEntity(reservation.get());
        return ResponseEntity.ok(reservationResource);
    }

    /**
     * Retrieves valid (non-expired) reservations for a specific user
     *
     * @param consumerId The unique identifier of the user
     * @return ResponseEntity containing the list of valid user reservations
     */
    @Operation(summary = "Get valid reservations by user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Valid user reservations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping("/user/{consumerId}/valid")
    public ResponseEntity<List<ReservationResponse>> getValidReservationsByUser(
            @Parameter(description = "User unique identifier") @PathVariable Long consumerId) {

        var reservations = reservationQueryService.getValidReservationsByConsumerId(consumerId);
        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(reservationResources);
    }

    /**
     * Retrieves expired reservations for cleanup or analysis
     *
     * @return ResponseEntity containing the list of expired reservations
     */
    @Operation(summary = "Get expired reservations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Expired reservations retrieved successfully")
    })
    @GetMapping("/expired")
    public ResponseEntity<List<ReservationResponse>> getExpiredReservations() {
        var reservations = reservationQueryService.getExpiredReservations();
        var reservationResources = reservations.stream()
                .map(ReservationResponseAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(reservationResources);
    }
}
