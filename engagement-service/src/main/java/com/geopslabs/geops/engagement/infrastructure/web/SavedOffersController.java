package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByUserIdAndOfferIdQuery;
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

/**
 * SavedOffersController
 *
 * REST controller for managing saved offer offers for users
 * This controller provides endpoints to create, retrieve, and delete saved offers
 *
 * @summary REST controller for saved offer management
 * @since 1.0
 * @author GeOps Labs
 */
@Tag(name = "Saved offers", description = "Offers the consumer saved to find them again")
@RestController
@RequestMapping(value = "/api/v1/saved-offers", produces = APPLICATION_JSON_VALUE)
public class SavedOffersController {

    private final SavedOfferCommandUseCase savedOfferCommandService;
    private final SavedOfferQueryUseCase savedOfferQueryService;

    /**
     * Constructor for dependency injection
     *
     * @param savedOfferCommandService Service for handling saved offer commands
     * @param savedOfferQueryService Service for handling saved offer queries
     */
    public SavedOffersController(SavedOfferCommandUseCase savedOfferCommandService,
                             SavedOfferQueryUseCase savedOfferQueryService) {
        this.savedOfferCommandService = savedOfferCommandService;
        this.savedOfferQueryService = savedOfferQueryService;
    }

    /**
     * Creates a new saved offer
     *
     * @param resource The saved offer creation request data
     * @return ResponseEntity containing the created saved offer or error status
     */
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

    /**
     * Retrieves saved offers by userId or checks if a specific saved offer exists
     *
     * @param userId The ID of the user whose saved offers are to be retrieved (required)
     * @param offerId The ID of the offer to check if saved offer (optional)
     * @return ResponseEntity containing the list of saved offers or specific saved offer, or error status
     */
    @Operation(summary = "Get savedOffers by userId or check if savedOffer exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SavedOffers retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "userId is required")
    })
    @GetMapping
    public ResponseEntity<?> getSavedOffers(
            @Parameter(description = "User ID (required)")
            @RequestParam(required = true) Long userId,
            @Parameter(description = "Offer ID (optional - for checking if savedOfferD)")
            @RequestParam(required = false) Long offerId) {

        if (offerId != null) {
            // GET /saved offers?userId=1&offerId=7
            // Check if specific saved offer exists (for heart button)
            var query = new GetSavedOfferByUserIdAndOfferIdQuery(userId, offerId);
            var savedOffer = savedOfferQueryService.handle(query);

            if (savedOffer.isPresent()) {
                var savedOfferResource = SavedOfferResponseAssembler
                    .toResourceFromEntity(savedOffer.get());
                return ResponseEntity.ok(savedOfferResource);
            } else {
                // Return 200 with empty body to indicate "not saved"
                return ResponseEntity.ok().build();
            }
        } else {
            // GET /saved offers?userId=1
            // Get all saved offers for user
            var query = new GetSavedOffersByConsumerQuery(userId);
            var savedOffers = savedOfferQueryService.handle(query);
            var savedOfferResources = savedOffers.stream()
                    .map(SavedOfferResponseAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(savedOfferResources);
        }
    }

    /**
     * Deletes a saved offer by ID
     *
     * @param id The unique identifier of the saved offer to delete
     * @return ResponseEntity with no content or error status
     */
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

    /**
     * Deletes a saved offer by userId and offerId
     * This endpoint is useful for the frontend when un-hearting an offer
     *
     * @param resource The delete saved offer resource containing userId and offerId
     * @return ResponseEntity with no content or error status
     */
    @Operation(summary = "Delete savedOffer by userId and offerId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "SavedOffer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "SavedOffer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteByUserIdAndOfferId(@RequestBody RemoveSavedOfferRequest resource) {
        var command = RemoveSavedOfferCommandAssembler.toCommandFromResource(resource);
        boolean deleted = savedOfferCommandService.handleDelete(command);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}

