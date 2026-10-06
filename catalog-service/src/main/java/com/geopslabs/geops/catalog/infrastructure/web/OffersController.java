package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetOfferByIdUseCase;
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

@Tag(name = "Offers", description = "Offers of the catalog, readable without an account")
@RestController
@RequestMapping(value = OffersController.OFFERS_PATH, produces = APPLICATION_JSON_VALUE)
public class OffersController {
    static final String OFFERS_PATH = "/api/v1/offers";

    private final GetOfferByIdUseCase getOfferById;

    public OffersController(GetOfferByIdUseCase getOfferById) {
        this.getOfferById = getOfferById;
    }

    @Operation(summary = "Get one offer of the catalog",
            description = "Public lookup of an offer with its conditions, price, validity and address.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Offer found",
            content = @Content(examples = @ExampleObject(value = """
                    {"offerId": 1052, "campaignId": 14, "businessId": 1, "title": "Menú ejecutivo a mitad de precio",
                     "conditions": "De lunes a viernes de 12:00 a 15:00. No acumulable.", "price": 12.50,
                     "validTo": "2026-10-31", "category": "Gastronomía", "address": "Jr. Huánuco 1250, La Victoria",
                     "imageUrl": "https://images.geops.pe/offers/1052.jpg", "source": "AFFILIATED",
                     "sourceName": null, "status": "PUBLISHED"}""")))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "offerId has an invalid value"}""")))
    @ApiResponse(responseCode = "404", description = "Offer does not exist",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "Offer 9999 was not found"}""")))
    @GetMapping("/{offerId}")
    public OfferResponse getOffer(
            @Parameter(description = "Offer id", example = "1052") @PathVariable Long offerId) {
        return OfferResponseAssembler.toResponse(getOfferById.getById(new GetOfferByIdQuery(offerId)));
    }
}
