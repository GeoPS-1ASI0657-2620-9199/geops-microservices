package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.SearchNearbyOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.SearchNearbyOffersQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Offers", description = "Offers of the catalog, readable without an account")
@RestController
@RequestMapping(value = NearbyOffersController.OFFERS_PATH, produces = APPLICATION_JSON_VALUE)
public class NearbyOffersController {
    static final String OFFERS_PATH = "/api/v1/offers";
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final SearchNearbyOffersUseCase searchNearbyOffers;

    public NearbyOffersController(SearchNearbyOffersUseCase searchNearbyOffers) {
        this.searchNearbyOffers = searchNearbyOffers;
    }

    @Operation(summary = "List valid offers within a walking radius, ordered by proximity",
            description = "Public. The radius is given in walking minutes (5 to 20) and converted at 80 m per minute.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Page of valid offers inside the radius",
            content = @Content(examples = @ExampleObject(value = """
                    {"content": [{"offerId": 1052, "title": "2x1 en almuerzos ejecutivos", "businessId": 84,
                      "businessName": "Restaurante Don Pepe", "verifiedSeal": true, "distanceMeters": 350,
                      "walkMinutes": 5, "category": "Gastronomía", "price": 15.00, "validTo": "2026-10-15"}],
                     "page": 0, "totalElements": 1, "totalPages": 1}""")))
    @ApiResponse(responseCode = "400",
            description = "Radius out of range, invalid coordinates, missing or invalid parameter, or invalid page",
            content = @Content(examples = {
                    @ExampleObject(name = "Radius out of range", value = """
                            {"code": "RADIUS_OUT_OF_RANGE", "message": "El radio debe estar entre 5 y 20 minutos a pie"}"""),
                    @ExampleObject(name = "Invalid coordinates", value = """
                            {"code": "INVALID_COORDINATES", "message": "La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180"}"""),
                    @ExampleObject(name = "Missing parameter", value = """
                            {"code": "MISSING_PARAMETER", "message": "Falta el parámetro radiusMinutes"}"""),
                    @ExampleObject(name = "Invalid parameter", value = """
                            {"code": "INVALID_PARAMETER", "message": "El parámetro lat no tiene un valor válido"}"""),
                    @ExampleObject(name = "Invalid page", value = """
                            {"code": "INVALID_PAGE", "message": "page debe ser 0 o mayor y size estar entre 1 y 20"}""")}))
    @GetMapping("/nearby")
    public NearbyOffersResponse searchNearby(
            @Parameter(description = "Latitude of the search origin", example = "-12.1211") @RequestParam double lat,
            @Parameter(description = "Longitude of the search origin", example = "-77.0297") @RequestParam double lng,
            @Parameter(description = "Walking radius in minutes, from 5 to 20", example = "10")
            @RequestParam int radiusMinutes,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @Parameter(description = "Page size, from 1 to 20", example = "20")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return NearbyOffersResponseAssembler.toResponse(searchNearbyOffers.searchNearbyOffers(
                new SearchNearbyOffersQuery(lat, lng, radiusMinutes, page, size)));
    }
}
