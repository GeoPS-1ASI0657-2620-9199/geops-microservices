package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GetCampaignByIdUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListBusinessCampaignsUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListCampaignOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignsByBusinessIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByCampaignIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Campaigns", description = "Advertising campaigns of the business in the token")
@RestController
@RequestMapping(value = CampaignsController.CAMPAIGNS_PATH, produces = APPLICATION_JSON_VALUE)
public class CampaignsController {
    static final String CAMPAIGNS_PATH = "/api/v1/campaigns";

    private static final String CAMPAIGN_EXAMPLE = """
            {"campaignId": 14, "businessId": 1, "name": "Almuerzos de octubre",
             "description": "Menú ejecutivo a mitad de precio para oficinas cercanas",
             "period": {"start": "2026-10-05", "end": "2026-10-31"},
             "zone": {"type": "RADIUS", "radiusMeters": 800, "district": null},
             "status": "ACTIVE", "estimatedBudget": {"amount": 500.00, "currency": "PEN"}}""";
    private static final String OFFER_EXAMPLE = """
            {"offerId": 1052, "campaignId": 14, "businessId": 1, "title": "Menú ejecutivo a mitad de precio",
             "conditions": "De lunes a viernes de 12:00 a 15:00. No acumulable.", "price": 12.50,
             "validTo": "2026-10-31", "category": "Gastronomía", "address": "Jr. Huánuco 1250, La Victoria",
             "imageUrl": null, "source": "AFFILIATED", "sourceName": null, "status": "PUBLISHED"}""";
    private static final String INVALID_ID_EXAMPLE = """
            {"code": "INVALID_REQUEST", "message": "campaignId has an invalid value"}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Campaign 14 belongs to another account"}""";
    private static final String NOT_FOUND_EXAMPLE = """
            {"code": "CAMPAIGN_NOT_FOUND", "message": "Campaign 9999 was not found"}""";

    private final ListBusinessCampaignsUseCase listBusinessCampaigns;
    private final GetCampaignByIdUseCase getCampaignById;
    private final ListCampaignOffersUseCase listCampaignOffers;

    public CampaignsController(ListBusinessCampaignsUseCase listBusinessCampaigns,
                               GetCampaignByIdUseCase getCampaignById,
                               ListCampaignOffersUseCase listCampaignOffers) {
        this.listBusinessCampaigns = listBusinessCampaigns;
        this.getCampaignById = getCampaignById;
        this.listCampaignOffers = listCampaignOffers;
    }

    @Operation(summary = "List the campaigns of the business in the token",
            description = "The business is the businessId claim of the token; the oldest campaign comes first.")
    @ApiResponse(responseCode = "200", description = "Campaigns of the business",
            content = @Content(examples = @ExampleObject(value = "[" + CAMPAIGN_EXAMPLE + "]")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""")))
    @GetMapping
    public List<CampaignResponse> listCampaigns(@AuthenticationPrincipal Jwt jwt) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        return listBusinessCampaigns.list(new GetCampaignsByBusinessIdQuery(businessId)).stream()
                .map(CampaignResponseAssembler::toResponse)
                .toList();
    }

    @Operation(summary = "Get one campaign of the business in the token")
    @ApiResponse(responseCode = "200", description = "Campaign found",
            content = @Content(examples = @ExampleObject(value = CAMPAIGN_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = INVALID_ID_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or campaign of another business",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Campaign does not exist",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/{campaignId}")
    public CampaignResponse getCampaign(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Campaign id", example = "14") @PathVariable Long campaignId) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var campaign = getCampaignById.getById(new GetCampaignByIdQuery(campaignId, businessId));
        return CampaignResponseAssembler.toResponse(campaign);
    }

    @Operation(summary = "List the offers of one campaign of the business in the token")
    @ApiResponse(responseCode = "200", description = "Offers of the campaign",
            content = @Content(examples = @ExampleObject(value = "[" + OFFER_EXAMPLE + "]")))
    @ApiResponse(responseCode = "400", description = "The id is not a number",
            content = @Content(examples = @ExampleObject(value = INVALID_ID_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or campaign of another business",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Campaign does not exist",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/{campaignId}/offers")
    public List<OfferResponse> listCampaignOffers(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Campaign id", example = "14") @PathVariable Long campaignId) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        return listCampaignOffers.list(new GetOffersByCampaignIdQuery(campaignId, businessId)).stream()
                .map(OfferResponseAssembler::toResponse)
                .toList();
    }
}
