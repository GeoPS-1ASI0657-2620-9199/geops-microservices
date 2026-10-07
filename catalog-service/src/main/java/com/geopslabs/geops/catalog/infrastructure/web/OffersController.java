package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetOfferDetailUseCase;
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

    private final GetOfferDetailUseCase getOfferDetail;

    public OffersController(GetOfferDetailUseCase getOfferDetail) {
        this.getOfferDetail = getOfferDetail;
    }

    @Operation(summary = "Get the detail of an offer with its conditions and merchant seal",
            description = "Public. available is false when the offer expired or is past its validity date in "
                    + "Lima time. An offer from a public source has no businessId, shows its source as "
                    + "businessName and never carries the verified seal. A removed offer answers 404.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Offer detail", content = @Content(examples = {
            @ExampleObject(name = "validOffer", value = """
                    {"offerId": 1052, "title": "2x1 en almuerzos ejecutivos",
                     "conditions": "Válido de lunes a viernes de 12:00 a 15:00. Un cupón por mesa.",
                     "price": 15.00, "validTo": "2026-10-15", "category": "Gastronomía",
                     "address": "Av. Larco 345, Miraflores", "latitude": -12.1211, "longitude": -77.0297,
                     "imageUrl": "https://images.geops.pe/offers/1052.jpg", "source": "AFFILIATED",
                     "businessId": 84, "businessName": "Restaurante Don Pepe", "verifiedSeal": false,
                     "available": true}"""),
            @ExampleObject(name = "expiredOffer", value = """
                    {"offerId": 1053, "title": "Desayuno 2x1", "conditions": "Solo hasta las 10:00.",
                     "price": 9.90, "validTo": "2026-10-01", "category": "Gastronomía",
                     "address": "Av. Larco 345, Miraflores", "latitude": -12.1211, "longitude": -77.0297,
                     "imageUrl": null, "source": "AFFILIATED", "businessId": 84,
                     "businessName": "Restaurante Don Pepe", "verifiedSeal": false, "available": false}""")}))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "offerId has an invalid value"}""")))
    @ApiResponse(responseCode = "404", description = "Offer not found or removed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "La oferta 9999 no existe."}""")))
    @GetMapping("/{offerId}")
    public OfferDetailResponse getOffer(
            @Parameter(description = "Offer id", example = "1052") @PathVariable Long offerId) {
        return OfferDetailResponseAssembler.toResponse(getOfferDetail.getDetail(new GetOfferByIdQuery(offerId)));
    }
}
