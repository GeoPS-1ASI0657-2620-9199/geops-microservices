package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetOfferByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListCampaignOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByCampaignIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Offers", description = "Offers of the catalog")
@RestController
@RequestMapping(value = "/api/v1/offers", produces = APPLICATION_JSON_VALUE)
public class OfferController {
    private final GetOfferByIdUseCase getOfferById;
    private final ListCampaignOffersUseCase listCampaignOffers;

    public OfferController(GetOfferByIdUseCase getOfferById, ListCampaignOffersUseCase listCampaignOffers) {
        this.getOfferById = getOfferById;
        this.listCampaignOffers = listCampaignOffers;
    }

    @Operation(summary = "Get offer by ID")
    @ApiResponse(responseCode = "200", description = "Offer found")
    @ApiResponse(responseCode = "404", description = "Offer not found")
    @GetMapping("/{id}")
    public OfferResource getById(@Parameter(description = "Offer unique identifier") @PathVariable Long id) {
        return OfferResourceFromEntityAssembler.toResourceFromEntity(getOfferById.getById(new GetOfferByIdQuery(id)));
    }

    @Operation(summary = "Get all offers from a campaign")
    @ApiResponse(responseCode = "200", description = "Offers of the campaign")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @GetMapping("/campaign/{id}")
    public List<OfferResource> getByCampaignId(@PathVariable Long id) {
        return listCampaignOffers.list(new GetOffersByCampaignIdQuery(id)).stream()
                .map(OfferResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
