package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReservationQueryUseCase;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByCodeQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationByIdQuery;
import com.geopslabs.geops.reservation.domain.models.queries.GetReservationsByConsumerIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Reservations", description = "Reservations of offers paid at the business")
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

    @Operation(summary = "Create a reservation")
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody CreateReservationRequest resource) {
        var command = CreateReservationCommandAssembler.toCommandFromResource(resource);
        var reservation = reservationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservationResponseAssembler.toResourceFromEntity(reservation));
    }

    @Operation(summary = "Get a reservation by id")
    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id) {
        var reservation = reservationQueryService.handle(new GetReservationByIdQuery(id));
        return ReservationResponseAssembler.toResourceFromEntity(reservation);
    }

    @Operation(summary = "List reservations, optionally of one consumer")
    @GetMapping
    public List<ReservationResponse> getAll(@RequestParam(required = false) Long consumerId) {
        var reservations = consumerId == null
                ? reservationQueryService.getAllReservations()
                : reservationQueryService.handle(new GetReservationsByConsumerIdQuery(consumerId));
        return reservations.stream().map(ReservationResponseAssembler::toResourceFromEntity).toList();
    }

    @Operation(summary = "List the reservations of a consumer")
    @GetMapping("/user/{consumerId}")
    public List<ReservationResponse> getByConsumer(@PathVariable Long consumerId) {
        var reservations = reservationQueryService.handle(new GetReservationsByConsumerIdQuery(consumerId));
        return reservations.stream().map(ReservationResponseAssembler::toResourceFromEntity).toList();
    }

    @Operation(summary = "Get a reservation by its code")
    @GetMapping("/code/{code}")
    public ReservationResponse getByCode(@PathVariable String code) {
        var reservation = reservationQueryService.handle(new GetReservationByCodeQuery(code));
        return ReservationResponseAssembler.toResourceFromEntity(reservation);
    }

    @Operation(summary = "List the reservations of a consumer that have not expired")
    @GetMapping("/user/{consumerId}/valid")
    public List<ReservationResponse> getValidByConsumer(@PathVariable Long consumerId) {
        var reservations = reservationQueryService.getValidReservationsByConsumerId(consumerId);
        return reservations.stream().map(ReservationResponseAssembler::toResourceFromEntity).toList();
    }
}
