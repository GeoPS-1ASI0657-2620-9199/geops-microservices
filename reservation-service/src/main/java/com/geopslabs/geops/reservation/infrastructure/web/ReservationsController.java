package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.application.usecases.CreateReservationUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import io.swagger.v3.oas.annotations.Operation;
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

    private final CreateReservationUseCase createReservation;
    private final ReservationQueryUseCase reservationQueryService;

    public ReservationsController(CreateReservationUseCase createReservation,
                                  ReservationQueryUseCase reservationQueryService) {
        this.createReservation = createReservation;
        this.reservationQueryService = reservationQueryService;
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
                    {"code": "VALIDATION_ERROR", "message": "offerId must not be null"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "UNAUTHORIZED", "message": "A valid token is required"}""")))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""")))
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

    @Operation(summary = "Get a reservation by id")
    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        var reservation = reservationQueryService.handle(new GetReservationByIdQuery(id));
        return ReservationResponseAssembler.toResponse(reservation);
    }

    @Operation(summary = "List reservations, optionally of one consumer")
    @GetMapping
    public List<ReservationResponse> getAll(@RequestParam(required = false) Long consumerId) {
        var reservations = consumerId == null
                ? reservationQueryService.getAllReservations()
                : reservationQueryService.handle(new GetReservationsByConsumerIdQuery(consumerId));
        return reservations.stream().map(ReservationResponseAssembler::toResponse).toList();
    }

    @Operation(summary = "List the reservations of a consumer")
    @GetMapping("/user/{consumerId}")
    public List<ReservationResponse> getByConsumer(@PathVariable Long consumerId) {
        var reservations = reservationQueryService.handle(new GetReservationsByConsumerIdQuery(consumerId));
        return reservations.stream().map(ReservationResponseAssembler::toResponse).toList();
    }

    @Operation(summary = "Get a reservation by its code")
    @GetMapping("/code/{code}")
    public ReservationResponse getByCode(@PathVariable String code) {
        var reservation = reservationQueryService.handle(new GetReservationByCodeQuery(code));
        return ReservationResponseAssembler.toResponse(reservation);
    }

    @Operation(summary = "List the reservations of a consumer that have not expired")
    @GetMapping("/user/{consumerId}/valid")
    public List<ReservationResponse> getValidByConsumer(@PathVariable Long consumerId) {
        var reservations = reservationQueryService.getValidReservationsByConsumerId(consumerId);
        return reservations.stream().map(ReservationResponseAssembler::toResponse).toList();
    }
}
