package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByConsumerIdAndOfferIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.infrastructure.web.SaveOfferRequest;
import com.geopslabs.geops.engagement.infrastructure.web.RemoveSavedOfferRequest;
import com.geopslabs.geops.engagement.infrastructure.web.SavedOfferResponse;
import com.geopslabs.geops.engagement.infrastructure.web.SaveOfferCommandAssembler;
import com.geopslabs.geops.engagement.infrastructure.web.RemoveSavedOfferCommandAssembler;
import com.geopslabs.geops.engagement.infrastructure.web.SavedOfferResponseAssembler;
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

@Tag(name = "Saved offers", description = "Offers the consumer saved to find them again")
@RestController
@RequestMapping(value = "/api/v1/saved-offers", produces = APPLICATION_JSON_VALUE)
public class SavedOffersController {
    private final SavedOfferCommandUseCase savedOfferCommandService;
    private final SavedOfferQueryUseCase savedOfferQueryService;

    public SavedOffersController(SavedOfferCommandUseCase savedOfferCommandService,
                             SavedOfferQueryUseCase savedOfferQueryService) {
        this.savedOfferCommandService = savedOfferCommandService;
        this.savedOfferQueryService = savedOfferQueryService;
    }

    @Operation(summary = "Create new savedOffer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "SavedOffer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or savedOffer already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<SavedOfferResponse> create(@RequestBody SaveOfferRequest resource) {
        var command = SaveOfferCommandAssembler.toCommandFromResource(resource);
        var savedOffer = savedOfferCommandService.handle(command);

        if (savedOffer.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var savedOfferResource = SavedOfferResponseAssembler.toResourceFromEntity(savedOffer.get());
        return new ResponseEntity<>(savedOfferResource, CREATED);
    }

    @Operation(summary = "Get savedOffers by consumerId or check if savedOffer exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SavedOffers retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "consumerId is required")
    })
    @GetMapping
    public ResponseEntity<?> getSavedOffers(
            @Parameter(description = "Consumer ID (required)")
            @RequestParam(required = true) Long consumerId,
            @Parameter(description = "Offer ID (optional - for checking if saved)")
            @RequestParam(required = false) Long offerId) {
        if (offerId != null) {
            var query = new GetSavedOfferByConsumerIdAndOfferIdQuery(consumerId, offerId);
            var savedOffer = savedOfferQueryService.handle(query);

            if (savedOffer.isPresent()) {
                var savedOfferResource = SavedOfferResponseAssembler
                    .toResourceFromEntity(savedOffer.get());
                return ResponseEntity.ok(savedOfferResource);
            } else {
                return ResponseEntity.ok().build();
            }
        } else {
            var query = new GetSavedOffersByConsumerQuery(consumerId);
            var savedOffers = savedOfferQueryService.handle(query);
            var savedOfferResources = savedOffers.stream()
                    .map(SavedOfferResponseAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(savedOfferResources);
        }
    }

    @Operation(summary = "Delete savedOffer by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "SavedOffer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "SavedOffer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid savedOffer ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "SavedOffer unique identifier") @PathVariable Long id) {
        boolean deleted = savedOfferCommandService.handleDelete(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete savedOffer by consumerId and offerId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "SavedOffer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "SavedOffer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteByConsumerIdAndOfferId(@RequestBody RemoveSavedOfferRequest resource) {
        var command = RemoveSavedOfferCommandAssembler.toCommandFromResource(resource);
        boolean deleted = savedOfferCommandService.handleDelete(command);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}

