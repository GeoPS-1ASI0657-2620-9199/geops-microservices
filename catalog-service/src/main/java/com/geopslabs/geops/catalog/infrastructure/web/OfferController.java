package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.DeleteOfferCommand;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByIdsQuery;
import com.geopslabs.geops.catalog.application.usecases.OfferCommandUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferQueryUseCase;
import com.geopslabs.geops.catalog.infrastructure.web.CreateOfferResource;
import com.geopslabs.geops.catalog.infrastructure.web.OfferResource;
import com.geopslabs.geops.catalog.infrastructure.web.UpdateOfferResource;
import com.geopslabs.geops.catalog.infrastructure.web.CreateOfferCommandFromResourceAssembler;
import com.geopslabs.geops.catalog.infrastructure.web.OfferResourceFromEntityAssembler;
import com.geopslabs.geops.catalog.infrastructure.web.UpdateOfferCommandFromResourceAssembler;
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

@Tag(name = "Offers", description = "Offer operations and management")
@RestController
@RequestMapping(value = "/api/v1/offers", produces = APPLICATION_JSON_VALUE)
public class OfferController {

    private final OfferCommandUseCase offerCommandService;
    private final OfferQueryUseCase offerQueryService;

    public OfferController(OfferCommandUseCase offerCommandService,
                          OfferQueryUseCase offerQueryService) {
        this.offerCommandService = offerCommandService;
        this.offerQueryService = offerQueryService;
    }

    @Operation(summary = "Create new offer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Offer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<OfferResource> create(@RequestBody CreateOfferResource resource) {
        var command = CreateOfferCommandFromResourceAssembler.toCommandFromResource(resource);
        var offer = offerCommandService.handle(command);

        if (offer.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var offerResource = OfferResourceFromEntityAssembler.toResourceFromEntity(offer.get());
        return new ResponseEntity<>(offerResource, CREATED);
    }

    @Operation(summary = "Get offer by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Offer found"),
        @ApiResponse(responseCode = "404", description = "Offer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid offer ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OfferResource> getById(
            @Parameter(description = "Offer unique identifier") @PathVariable Long id) {
        var query = new GetOfferByIdQuery(id);
        var offer = offerQueryService.handle(query);

        if (offer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var offerResource = OfferResourceFromEntityAssembler.toResourceFromEntity(offer.get());
        return ResponseEntity.ok(offerResource);
    }

    @Operation(summary = "Get all offers or offers by IDs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Offers retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<OfferResource>> getOffers(
            @Parameter(description = "Optional list of offer IDs") @RequestParam(required = false) List<Long> id) {

        List<OfferResource> offerResources;

        if (id != null && !id.isEmpty()) {
            var query = new GetOffersByIdsQuery(id);
            var offers = offerQueryService.handle(query);
            offerResources = offers.stream()
                    .map(OfferResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
        } else {
            var query = new GetAllOffersQuery();
            var offers = offerQueryService.handle(query);
            offerResources = offers.stream()
                    .map(OfferResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
        }

        return ResponseEntity.ok(offerResources);
    }

    @Operation(summary = "Update offer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Offer updated successfully"),
        @ApiResponse(responseCode = "404", description = "Offer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OfferResource> update(
            @Parameter(description = "Offer unique identifier") @PathVariable Long id,
            @RequestBody UpdateOfferResource resource) {

        var existingOfferQuery = new GetOfferByIdQuery(id);
        var existingOffer = offerQueryService.handle(existingOfferQuery);

        if (existingOffer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var updateCommand = UpdateOfferCommandFromResourceAssembler.toCommandFromResource(id, resource);
        var offer = offerCommandService.handle(updateCommand);

        if (offer.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var offerResource = OfferResourceFromEntityAssembler.toResourceFromEntity(offer.get());
        return ResponseEntity.ok(offerResource);
    }

    @Operation(summary = "Delete offer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Offer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Offer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid offer ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Offer unique identifier") @PathVariable Long id) {

        boolean deleted = offerCommandService.handle(new DeleteOfferCommand(id));

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all offers from a campaign", description = "Get all offers by using a campaign unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Offers not found")
    })
    @GetMapping("/campaign/{id}")
    public ResponseEntity<List<OfferResource>> getByCampaignId(@PathVariable Long id){
        var query = new GetAllOffersByCampaignIdQuery(id);

        var offers = offerQueryService.handle(query);
        if(offers.isEmpty()) return ResponseEntity.notFound().build();
        var offersResource =  offers.stream()
                .map(OfferResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(offersResource);
    }
}
