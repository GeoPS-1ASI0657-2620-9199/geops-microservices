package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.CreateCampaignUseCase;
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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
             "zone": {"type": "RADIUS", "center": {"latitude": -12.1211, "longitude": -77.0297},
                      "radiusMeters": 800, "district": null},
             "status": "ACTIVE", "estimatedBudget": {"amount": 500.00, "currency": "PEN"}}""";
    private static final String CREATE_CAMPAIGN_EXAMPLE = """
            {"businessName": "Restaurante Don Pepe", "name": "Almuerzos de octubre",
             "description": "Menú ejecutivo a mitad de precio para oficinas cercanas",
             "period": {"start": "2026-10-05", "end": "2026-10-31"},
             "estimatedBudget": {"amount": 500.00, "currency": "PEN"},
             "storeLocation": {"address": "Av. Larco 345, Miraflores", "latitude": -12.1211, "longitude": -77.0297},
             "zone": {"type": "RADIUS", "center": {"latitude": -12.1211, "longitude": -77.0297}, "radiusMeters": 800},
             "offers": [{"title": "2x1 en almuerzos ejecutivos",
                         "conditions": "Válido de lunes a viernes de 12:00 a 15:00. Un cupón por mesa.",
                         "price": 15.00, "validTo": "2026-10-15", "category": "Gastronomía",
                         "imageUrl": "https://images.geops.pe/offers/1052.jpg"}]}""";
    private static final String PUBLISHED_CAMPAIGN_EXAMPLE = """
            {"campaignId": 31, "businessId": 84, "name": "Almuerzos de octubre", "status": "ACTIVE",
             "period": {"start": "2026-10-05", "end": "2026-10-31"},
             "zone": {"type": "RADIUS", "center": {"latitude": -12.1211, "longitude": -77.0297},
                      "radiusMeters": 800, "district": null},
             "offers": [{"offerId": 1052, "title": "2x1 en almuerzos ejecutivos", "validTo": "2026-10-15",
                         "status": "PUBLISHED"}]}""";
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

    private final CreateCampaignUseCase createCampaign;
    private final ListBusinessCampaignsUseCase listBusinessCampaigns;
    private final GetCampaignByIdUseCase getCampaignById;
    private final ListCampaignOffersUseCase listCampaignOffers;

    public CampaignsController(CreateCampaignUseCase createCampaign,
                               ListBusinessCampaignsUseCase listBusinessCampaigns,
                               GetCampaignByIdUseCase getCampaignById,
                               ListCampaignOffersUseCase listCampaignOffers) {
        this.createCampaign = createCampaign;
        this.listBusinessCampaigns = listBusinessCampaigns;
        this.getCampaignById = getCampaignById;
        this.listCampaignOffers = listCampaignOffers;
    }

    @Operation(summary = "Create a geo-referenced campaign with its offers",
            description = "Requires ROLE_BUSINESS_OWNER. The campaign belongs to the businessId of the token, "
                    + "never to an id in the body. businessName creates the local copy of the business the first "
                    + "time (until BusinessRegistered arrives with US33). Each offer is published with the store "
                    + "address and coordinates. Without estimatedBudget the budget is 0.00 PEN. This step accepts "
                    + "radius zones; district zones arrive with US06.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(
            name = "radiusZone", value = CREATE_CAMPAIGN_EXAMPLE)))
    @ApiResponse(responseCode = "201", description = "Campaign published with its offers",
            content = @Content(examples = @ExampleObject(value = PUBLISHED_CAMPAIGN_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Invalid request, ended period, invalid zone or offer outside "
            + "the campaign", content = @Content(examples = {
                    @ExampleObject(name = "campaignAlreadyEnded", value = """
                            {"code": "CAMPAIGN_ALREADY_ENDED", "message": "La vigencia de la campaña terminó el 2026-09-30. Elige una fecha de fin desde hoy."}"""),
                    @ExampleObject(name = "invalidPeriod", value = """
                            {"code": "INVALID_CAMPAIGN_PERIOD", "message": "La fecha de fin de la campaña es anterior a su fecha de inicio."}"""),
                    @ExampleObject(name = "invalidZone", value = """
                            {"code": "INVALID_CAMPAIGN_ZONE", "message": "El radio de la zona debe estar entre 400 y 5000 metros y tener un centro."}"""),
                    @ExampleObject(name = "offerOutsideCampaign", value = """
                            {"code": "OFFER_VALIDITY_OUTSIDE_CAMPAIGN", "message": "La vigencia de la oferta «Desayuno 2x1» debe estar dentro del periodo de la campaña."}"""),
                    @ExampleObject(name = "invalidRequest", value = """
                            {"code": "INVALID_REQUEST", "message": "offers must not be empty"}""")}))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or without businessId",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""")))
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<PublishedCampaignResponse> createCampaign(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCampaignRequest request) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var published = createCampaign.publish(request.toCommand(businessId));
        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignResponseAssembler.toPublishedResponse(published));
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
