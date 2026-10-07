package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetOfferAvailabilityUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Internal", description = "Operations for other GeoPS services; the gateway does not publish them")
@RestController
@RequestMapping(value = InternalOffersController.INTERNAL_OFFERS_PATH, produces = APPLICATION_JSON_VALUE)
public class InternalOffersController {
    static final String INTERNAL_OFFERS_PATH = "/internal/v1/offers";

    private final GetOfferAvailabilityUseCase getOfferAvailability;

    public InternalOffersController(GetOfferAvailabilityUseCase getOfferAvailability) {
        this.getOfferAvailability = getOfferAvailability;
    }

    @Operation(summary = "Tell Reservation whether an offer can be reserved today",
            description = "Internal. Not routed by the gateway. available is true only for an affiliated offer "
                    + "that is PUBLISHED and whose validTo is today or later in Lima; a public source offer "
                    + "answers available false and no businessId. reservation-service reads businessId, title "
                    + "and validTo to keep a copy of the offer with the reservation.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Offer found",
            content = @Content(examples = {
                    @ExampleObject(name = "available", value = """
                            {"offerId": 1052, "businessId": 84, "title": "2x1 en almuerzos ejecutivos",
                             "validTo": "2026-10-15", "available": true}"""),
                    @ExampleObject(name = "notAvailable", value = """
                            {"offerId": 1053, "businessId": 84, "title": "Desayuno 2x1",
                             "validTo": "2026-10-06", "available": false}""")}))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "offerId has an invalid value"}""")))
    @ApiResponse(responseCode = "404", description = "Offer does not exist",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "La oferta 9999 no existe."}""")))
    @GetMapping("/{offerId}/availability")
    public OfferAvailabilityResponse getAvailability(
            @Parameter(description = "Offer id", example = "1052") @PathVariable Long offerId) {
        var availability = getOfferAvailability.getAvailability(new GetOfferByIdQuery(offerId));
        return new OfferAvailabilityResponse(availability.offerId(), availability.businessId(), availability.title(),
                availability.validTo(), availability.available());
    }
}
