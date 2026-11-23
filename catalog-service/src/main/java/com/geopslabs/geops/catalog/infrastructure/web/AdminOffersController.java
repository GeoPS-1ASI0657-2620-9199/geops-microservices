package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.ListModerationHistoryUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListOffersForModerationUseCase;
import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersUseCase;
import com.geopslabs.geops.catalog.application.usecases.RemoveOfferCommand;
import com.geopslabs.geops.catalog.application.usecases.RemoveOfferUseCase;
import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Administration", description = "Operations of the GeoPS administrator (ROLE_ADMIN)")
@RestController
@RequestMapping(value = "/api/v1/admin", produces = APPLICATION_JSON_VALUE)
public class AdminOffersController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final LoadPublicOffersUseCase loadPublicOffers;
    private final RemoveOfferUseCase removeOffer;
    private final ListOffersForModerationUseCase listOffers;
    private final ListModerationHistoryUseCase listHistory;

    public AdminOffersController(LoadPublicOffersUseCase loadPublicOffers, RemoveOfferUseCase removeOffer,
                                 ListOffersForModerationUseCase listOffers, ListModerationHistoryUseCase listHistory) {
        this.loadPublicOffers = loadPublicOffers;
        this.removeOffer = removeOffer;
        this.listOffers = listOffers;
        this.listHistory = listHistory;
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

    @Operation(summary = "Remove an offer that breaks the publication rules (US24)",
            description = "The reason is mandatory. The offer is kept with status REMOVED, so reservations keep "
                    + "their reference, but it no longer appears in the search and its detail answers 404. The "
                    + "action records which administrator removed it and when. An offer of a campaign also writes "
                    + "OfferRemoved to the outbox, and its cached cell is deleted so the next search is up to date.")
    @ApiResponse(responseCode = "200", description = "Offer removed and action recorded",
            content = @Content(examples = @ExampleObject(value = """
                    {"moderationId": 31, "offerId": 1052, "offerStatus": "REMOVED", "adminId": 1,
                     "reason": "La oferta anuncia un precio que el local no respeta.",
                     "removedAt": "2026-11-22T15:04:05Z"}""")))
    @ApiResponse(responseCode = "400", description = "Missing reason, or shorter than 10 or longer than 255 "
            + "characters", content = @Content(examples = @ExampleObject(value = """
            {"code": "INVALID_REQUEST", "message": "reason size must be between 10 and 255"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @ApiResponse(responseCode = "404", description = "The offer does not exist",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_NOT_FOUND", "message": "La oferta 1052 no existe."}""")))
    @ApiResponse(responseCode = "409", description = "The offer was already removed",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "OFFER_ALREADY_REMOVED", "message": "La oferta 1052 ya fue retirada del catálogo."}""")))
    @DeleteMapping(value = "/offers/{offerId}", consumes = APPLICATION_JSON_VALUE)
    public ModerationActionResponse removeOffer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long offerId,
                                                @Valid @RequestBody RemoveOfferRequest request) {
        var admin = AuthenticatedUser.from(jwt);
        return ModerationActionResponse.from(
                removeOffer.remove(new RemoveOfferCommand(offerId, admin.userId(), request.reason())));
    }

    @Operation(summary = "List the offers of the catalog with their status for the moderation panel (US24)",
            description = "Newest first. status may be repeated; without it every status is listed. reported=true "
                    + "keeps the offers whose business has open reports. q matches the title or the business name, "
                    + "ignoring case. businessName is the source name for a public source offer.")
    @ApiResponse(responseCode = "200", description = "One page of offers",
            content = @Content(examples = @ExampleObject(value = """
                    {"content": [{"offerId": 1052, "title": "2x1 en almuerzos ejecutivos",
                       "category": "Gastronomía", "source": "AFFILIATED", "businessId": 84,
                       "businessName": "Restaurante Don Pepe", "status": "PUBLISHED", "validTo": "2026-11-30",
                       "openReports": 2, "verifiedSeal": false}],
                     "page": 0, "totalElements": 1, "totalPages": 1}""")))
    @ApiResponse(responseCode = "400", description = "Unknown status or invalid page",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_PAGE", "message": "page debe ser 0 o mayor y size estar entre 1 y 50"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @GetMapping("/offers")
    public ModerationOffersResponse listOffers(
            @Parameter(description = "PUBLISHED, EXPIRED or REMOVED; repeat it to ask for several")
            @RequestParam(name = "status", required = false) List<OfferStatus> statuses,
            @Parameter(description = "Only offers whose business has open reports", example = "false")
            @RequestParam(defaultValue = "false") boolean reported,
            @Parameter(description = "Text to find in the title or the business name", example = "almuerzo")
            @RequestParam(required = false) String q,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @Parameter(description = "Page size, from 1 to 50", example = "20")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        var wanted = statuses == null ? Set.<OfferStatus>of() : Set.copyOf(statuses);
        return ModerationOffersResponse.from(
                listOffers.listOffers(new ModerationFilter(wanted, reported, q), page, size));
    }

    @Operation(summary = "List the moderation history, the most recent first (US24)",
            description = "Each row says which offer was removed, by which administrator, when and why.")
    @ApiResponse(responseCode = "200", description = "One page of moderation actions",
            content = @Content(examples = @ExampleObject(value = """
                    {"content": [{"moderationId": 31, "offerId": 1052, "offerTitle": "2x1 en almuerzos ejecutivos",
                       "adminId": 1, "reason": "La oferta anuncia un precio que el local no respeta.",
                       "removedAt": "2026-11-22T15:04:05Z"}],
                     "page": 0, "totalElements": 1, "totalPages": 1}""")))
    @ApiResponse(responseCode = "400", description = "Invalid page")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @GetMapping("/moderation-actions")
    public ModerationHistoryResponse listModerationHistory(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @Parameter(description = "Page size, from 1 to 50", example = "20")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        return ModerationHistoryResponse.from(listHistory.listHistory(page, size));
    }

    public record LoadedPublicOffersResponse(int created, List<Long> offerIds) {
    }
}
