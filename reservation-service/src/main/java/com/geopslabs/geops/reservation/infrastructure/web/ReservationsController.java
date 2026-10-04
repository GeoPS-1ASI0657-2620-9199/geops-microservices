package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
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

@Tag(name = "Reservations", description = "Reservation operations and management")
@RestController
@RequestMapping(value = "/api/v1/reservations", produces = APPLICATION_JSON_VALUE)
public class ReservationsController {

    private final ReservationCommandUseCase reservationCommandService;
    private final ReservationQueryUseCase reservationQueryService;

    public ReservationsController(ReservationCommandUseCase reservationCommandService,
                           ReservationQueryUseCase reservationQueryService) {
        this.reservationCommandService = reservationCommandService;
        this.reservationQueryService = reservationQueryService;
    }

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

            var existingReservationQuery = new GetReservationByIdQuery(reservationId);
            var existingReservation = reservationQueryService.handle(existingReservationQuery);

            if (existingReservation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var updateCommand = new UpdateReservationCommand(
                reservationId, resource.offerId(),
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
