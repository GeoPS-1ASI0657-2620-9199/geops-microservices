package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "Save an offer for a consumer")
    @PostMapping
    public ResponseEntity<SavedOfferResponse> create(@RequestBody SaveOfferRequest request) {
        var savedOffer = savedOfferCommandService.handle(SaveOfferCommandAssembler.toCommandFromResource(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SavedOfferResponseAssembler.toResourceFromEntity(savedOffer));
    }

    @Operation(summary = "List the saved offers of a consumer")
    @GetMapping
    public List<SavedOfferResponse> getSavedOffers(
            @Parameter(description = "Consumer ID") @RequestParam Long consumerId) {
        return savedOfferQueryService.handle(new GetSavedOffersByConsumerQuery(consumerId)).stream()
                .map(SavedOfferResponseAssembler::toResourceFromEntity)
                .toList();
    }

    @Operation(summary = "Remove a saved offer of a consumer")
    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> delete(@Parameter(description = "Consumer ID") @RequestParam Long consumerId,
                                       @Parameter(description = "Offer ID") @PathVariable Long offerId) {
        savedOfferCommandService.handle(new RemoveSavedOfferCommand(consumerId, offerId));
        return ResponseEntity.noContent().build();
    }
}
