package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Administration", description = "Operations of the GeoPS administrator (ROLE_ADMIN)")
@RestController
@RequestMapping(value = "/api/v1/admin", produces = APPLICATION_JSON_VALUE)
public class AdminOffersController {
    private final LoadPublicOffersUseCase loadPublicOffers;

    public AdminOffersController(LoadPublicOffersUseCase loadPublicOffers) {
        this.loadPublicOffers = loadPublicOffers;
    }

    @Operation(summary = "Load a batch of public source offers",
            description = "From 1 to 200 offers. Each one is saved PUBLISHED, without coordinates and with "
                    + "geocoding_status PENDING until a geocoding run finds its address (US07, E2-09).")
    @ApiResponse(responseCode = "201", description = "Offers saved",
            content = @Content(examples = @ExampleObject(value = """
                    {"created": 2, "offerIds": [9001, 9002]}""")))
    @ApiResponse(responseCode = "400", description = "Empty batch, more than 200 offers, a missing field or an "
            + "unknown category", content = @Content(examples = @ExampleObject(value = """
            {"code": "INVALID_CATEGORY", "message": "La categoría no existe. Las categorías válidas son: Gastronomía, ..."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @PostMapping(value = "/public-offers", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LoadedPublicOffersResponse loadPublicOffers(@Valid @RequestBody LoadPublicOffersRequest request) {
        var ids = loadPublicOffers.load(request.toCommand());
        return new LoadedPublicOffersResponse(ids.size(), ids);
    }

    public record LoadedPublicOffersResponse(int created, List<Long> offerIds) {
    }
}
