package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.ListSavedOffersUseCase;
import com.geopslabs.geops.engagement.application.usecases.RemoveSavedOfferUseCase;
import com.geopslabs.geops.engagement.application.usecases.SaveOfferUseCase;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Saved offers", description = "Offers the consumer saved to find them again")
@RestController
@RequestMapping(value = SavedOffersController.SAVED_OFFERS_PATH, produces = APPLICATION_JSON_VALUE)
public class SavedOffersController {
    static final String SAVED_OFFERS_PATH = "/api/v1/saved-offers";

    private static final String SAVED_OFFER_EXAMPLE = """
            {"savedOfferId": 9, "offerId": 1, "businessId": 1, "businessName": "Bodega Doña Rosa",
             "title": "Menú ejecutivo a mitad de precio", "validTo": "2026-10-31", "expired": false,
             "savedAt": "2026-10-08T13:05:00Z"}""";
    private static final String EXPIRED_SAVED_OFFER_EXAMPLE = """
            {"savedOfferId": 7, "offerId": 2, "businessId": 1, "businessName": "Bodega Doña Rosa",
             "title": "Desayuno criollo a S/ 8", "validTo": "2026-09-30", "expired": true,
             "savedAt": "2026-09-28T15:40:00Z"}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";

    private final SaveOfferUseCase saveOffer;
    private final ListSavedOffersUseCase listSavedOffers;
    private final RemoveSavedOfferUseCase removeSavedOffer;

    public SavedOffersController(SaveOfferUseCase saveOffer, ListSavedOffersUseCase listSavedOffers,
                                 RemoveSavedOfferUseCase removeSavedOffer) {
        this.saveOffer = saveOffer;
        this.listSavedOffers = listSavedOffers;
        this.removeSavedOffer = removeSavedOffer;
    }

    @Operation(summary = "Save an offer for the consumer of the token",
            description = "Reads the offer from the local snapshot. Saving the same offer again returns the existing one.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "saveOffer", value = """
                    {"offerId": 1}""")))
    @ApiResponse(responseCode = "201", description = "Offer saved",
            content = @Content(examples = @ExampleObject(value = SAVED_OFFER_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "The offer was already saved; same body",
            content = @Content(examples = @ExampleObject(value = SAVED_OFFER_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Missing offerId",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: offerId."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "No local snapshot of the offer",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "No encontramos esa oferta."}""")))
    @ApiResponse(responseCode = "409", description = "The offer already expired",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_AVAILABLE", "message": "Esa oferta ya no está vigente."}""")))
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedOfferResponse> saveOffer(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveOfferRequest request) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        var result = saveOffer.save(request.toCommand(consumerId));
        var body = SavedOfferResponseAssembler.toResponse(result.savedOffer());
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(body);
    }

    @Operation(summary = "List the saved offers of the consumer of the token, newest first",
            description = "Offers that expired after being saved come with expired set to true.")
    @ApiResponse(responseCode = "200", description = "Saved offers of the consumer",
            content = @Content(examples = @ExampleObject(
                    value = "[" + SAVED_OFFER_EXAMPLE + ", " + EXPIRED_SAVED_OFFER_EXAMPLE + "]")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @GetMapping
    public List<SavedOfferResponse> listSavedOffers(@AuthenticationPrincipal Jwt jwt) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        return listSavedOffers.list(new GetSavedOffersByConsumerQuery(consumerId)).stream()
                .map(SavedOfferResponseAssembler::toResponse)
                .toList();
    }

    @Operation(summary = "Remove an offer from the saved offers of the consumer of the token")
    @ApiResponse(responseCode = "204", description = "Saved offer removed")
    @ApiResponse(responseCode = "400", description = "The offer id is not a number",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: offerId."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "The consumer had not saved that offer",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "SAVED_OFFER_NOT_FOUND", "message": "No tienes guardada esa oferta."}""")))
    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> removeSavedOffer(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Offer id", example = "1") @PathVariable Long offerId) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        removeSavedOffer.remove(new RemoveSavedOfferCommand(consumerId, offerId));
        return ResponseEntity.noContent().build();
    }
}
