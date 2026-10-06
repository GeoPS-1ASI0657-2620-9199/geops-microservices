package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.application.usecases.CreateReservationUseCase;
import com.geopslabs.geops.reservation.application.usecases.GetReservationByCodeUseCase;
import com.geopslabs.geops.reservation.application.usecases.GetReservationByIdUseCase;
import com.geopslabs.geops.reservation.application.usecases.ListConsumerReservationsUseCase;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reservations", description = "Reservations of offers paid at the business")
@RestController
@RequestMapping(value = ReservationsController.RESERVATIONS_PATH, produces = APPLICATION_JSON_VALUE)
public class ReservationsController {
    static final String RESERVATIONS_PATH = "/api/v1/reservations";
    private static final String PATH_SEPARATOR = "/";

    private static final String RESERVATION_EXAMPLE = """
            {"reservationId": 15, "code": "K7P3XM9Q", "consumerId": 2001, "offerId": 1052, "businessId": 301,
             "offerTitle": "Menú ejecutivo a mitad de precio", "status": "ACTIVE",
             "reservedAt": "2026-10-08T13:05:00Z", "expiresAt": "2026-10-15T04:59:59Z", "redeemedAt": null}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";

    private final CreateReservationUseCase createReservation;
    private final GetReservationByIdUseCase getReservationById;
    private final GetReservationByCodeUseCase getReservationByCode;
    private final ListConsumerReservationsUseCase listConsumerReservations;

    public ReservationsController(CreateReservationUseCase createReservation,
                                  GetReservationByIdUseCase getReservationById,
                                  GetReservationByCodeUseCase getReservationByCode,
                                  ListConsumerReservationsUseCase listConsumerReservations) {
        this.createReservation = createReservation;
        this.getReservationById = getReservationById;
        this.getReservationByCode = getReservationByCode;
        this.listConsumerReservations = listConsumerReservations;
    }

    @Operation(summary = "Create a reservation for a valid offer, paid later at the business",
            description = "The consumer is the subject of the token. Repeating the request while the reservation "
                    + "is active returns the same reservation with 200.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
            {"offerId": 1052}""")))
    @ApiResponse(responseCode = "201", description = "Reservation created",
            content = @Content(examples = @ExampleObject(value = """
                    {"reservationId": 15, "code": "K7P3XM9Q", "expiresAt": "2026-10-15T04:59:59Z"}""")))
    @ApiResponse(responseCode = "200", description = "Active reservation for this offer already existed",
            content = @Content(examples = @ExampleObject(value = """
                    {"reservationId": 15, "code": "K7P3XM9Q", "expiresAt": "2026-10-15T04:59:59Z"}""")))
    @ApiResponse(responseCode = "400", description = "Missing offerId",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "offerId must not be null"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Offer does not exist",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "Offer 9999 was not found"}""")))
    @ApiResponse(responseCode = "409", description = "Offer is no longer valid",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_AVAILABLE", "message": "Offer 1053 is no longer valid"}""")))
    @ApiResponse(responseCode = "503", description = "Catalog did not answer",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "CATALOG_UNAVAILABLE", "message": "Catalog is not available"}""")))
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedReservationResponse> createReservation(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReservationRequest request) {
        var command = CreateReservationCommandAssembler.toCommand(AuthenticatedUser.from(jwt), request);
        var result = createReservation.create(command);
        var body = ReservationResponseAssembler.toCreatedResponse(result.reservation());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        var location = URI.create(RESERVATIONS_PATH + PATH_SEPARATOR + body.reservationId());
        return ResponseEntity.created(location).body(body);
    }

    @Operation(summary = "Get one reservation of the consumer in the token")
    @ApiResponse(responseCode = "200", description = "Reservation found",
            content = @Content(examples = @ExampleObject(value = RESERVATION_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "id has an invalid value"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER or reservation of another consumer",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Reservation 15 belongs to another account"}""")))
    @ApiResponse(responseCode = "404", description = "Reservation does not exist",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RESERVATION_NOT_FOUND", "message": "Reservation 9999 was not found"}""")))
    @GetMapping("/{id}")
    public ReservationResponse getReservation(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Reservation id", example = "15") @PathVariable Long id) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        var reservation = getReservationById.getById(new GetReservationByIdQuery(id, consumerId));
        return ReservationResponseAssembler.toResponse(reservation);
    }

    @Operation(summary = "Get the reservation that matches a code, for the business that owns the offer")
    @ApiResponse(responseCode = "200", description = "Reservation found",
            content = @Content(examples = @ExampleObject(value = RESERVATION_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or code of another business",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Reservation 15 belongs to another account"}""")))
    @ApiResponse(responseCode = "404", description = "No reservation has this code",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RESERVATION_NOT_FOUND", "message": "Reservation with code ZZZZ2222 was not found"}""")))
    @GetMapping("/code/{code}")
    public ReservationResponse getReservationByCode(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Reservation code", example = "K7P3XM9Q") @PathVariable String code) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var reservation = getReservationByCode.getByCode(new GetReservationByCodeQuery(code, businessId));
        return ReservationResponseAssembler.toResponse(reservation);
    }

    @Operation(summary = "List the reservations of the consumer in the token, newest first")
    @ApiResponse(responseCode = "200", description = "Reservations of the consumer",
            content = @Content(examples = @ExampleObject(value = "[" + RESERVATION_EXAMPLE + "]")))
    @ApiResponse(responseCode = "400", description = "Unknown status",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "status has an invalid value"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @GetMapping
    public List<ReservationResponse> listReservations(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Only reservations in this status", example = "ACTIVE")
            @RequestParam(required = false) ReservationStatus status) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        var reservations = listConsumerReservations.list(new GetReservationsByConsumerIdQuery(consumerId, status));
        return reservations.stream().map(ReservationResponseAssembler::toResponse).toList();
    }
}
