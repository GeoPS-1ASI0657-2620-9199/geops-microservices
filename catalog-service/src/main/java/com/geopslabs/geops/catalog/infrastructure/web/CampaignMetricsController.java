package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.BusinessBaselineUseCase;
import com.geopslabs.geops.catalog.application.usecases.GetCampaignMetricsUseCase;
import com.geopslabs.geops.catalog.application.usecases.ListCampaignMetricsUseCase;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignMetricsQuery;
import com.geopslabs.geops.catalog.domain.models.queries.ListCampaignMetricsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Metrics of the campaigns (US11) and baseline of the business (US45). They live under /api/v1/campaigns because
 * the gateway already routes that prefix to Catalog and the security already restricts it to ROLE_BUSINESS_OWNER;
 * the baseline is the reference the campaigns are compared with.
 */
@Tag(name = "Campaign metrics", description = "Reach, clicks and conversions of the campaigns and the baseline of "
        + "the business in the token")
@RestController
@RequestMapping(value = CampaignsController.CAMPAIGNS_PATH, produces = APPLICATION_JSON_VALUE)
public class CampaignMetricsController {
    private static final String METRICS_EXAMPLE = """
            {"campaignId": 14, "campaignName": "Almuerzos de octubre", "status": "ACTIVE",
             "period": {"from": "2026-10-05", "to": "2026-10-07", "days": 3},
             "totals": {"impressions": 640, "clicks": 48, "reservations": 12, "redemptions": 9},
             "rates": {"clickThroughRate": 7.5, "reservationRate": 25.0, "redemptionRate": 75.0,
                       "conversionRate": 18.8},
             "daily": [
               {"date": "2026-10-05", "impressions": 210, "clicks": 15, "reservations": 4, "redemptions": 3},
               {"date": "2026-10-06", "impressions": 0, "clicks": 0, "reservations": 0, "redemptions": 0},
               {"date": "2026-10-07", "impressions": 430, "clicks": 33, "reservations": 8, "redemptions": 6}],
             "baseline": {"customersPerWeek": 140, "averageTicket": 18.50, "declaredAt": "2026-10-01T15:20:00Z",
                          "expectedCustomers": 60.0, "campaignCustomers": 9, "customersChangePercent": 15.0,
                          "estimatedExtraSales": 166.50}}""";
    private static final String METRICS_WITHOUT_BASELINE_EXAMPLE = """
            {"campaignId": 15, "campaignName": "Ceviches en Miraflores", "status": "PAUSED",
             "period": {"from": "2026-10-05", "to": "2026-10-05", "days": 1},
             "totals": {"impressions": 0, "clicks": 0, "reservations": 0, "redemptions": 0},
             "rates": {"clickThroughRate": null, "reservationRate": null, "redemptionRate": null,
                       "conversionRate": null},
             "daily": [{"date": "2026-10-05", "impressions": 0, "clicks": 0, "reservations": 0, "redemptions": 0}],
             "baseline": null}""";
    private static final String SUMMARY_EXAMPLE = """
            {"period": {"from": "2026-09-08", "to": "2026-10-07", "days": 30},
             "campaigns": [
               {"campaignId": 14, "name": "Almuerzos de octubre", "status": "ACTIVE",
                "validity": {"start": "2026-10-05", "end": "2026-10-31"},
                "totals": {"impressions": 640, "clicks": 48, "reservations": 12, "redemptions": 9},
                "rates": {"clickThroughRate": 7.5, "reservationRate": 25.0, "redemptionRate": 75.0,
                          "conversionRate": 18.8}},
               {"campaignId": 15, "name": "Ceviches en Miraflores", "status": "PAUSED",
                "validity": {"start": "2026-10-05", "end": "2026-10-20"},
                "totals": {"impressions": 0, "clicks": 0, "reservations": 0, "redemptions": 0},
                "rates": {"clickThroughRate": null, "reservationRate": null, "redemptionRate": null,
                          "conversionRate": null}}]}""";
    private static final String BASELINE_EXAMPLE = """
            {"businessId": 84, "customersPerWeek": 140, "averageTicket": 18.50,
             "declaredAt": "2026-10-01T15:20:00Z"}""";
    private static final String DECLARE_BASELINE_EXAMPLE = """
            {"customersPerWeek": 140, "averageTicket": 18.50}""";
    private static final String INVALID_PERIOD_EXAMPLE = """
            {"code": "INVALID_METRICS_PERIOD", "message": "La fecha final del periodo es anterior a la fecha inicial."}""";
    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Campaign 14 belongs to another account"}""";
    private static final String OWNER_ONLY_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";
    private static final String NOT_FOUND_EXAMPLE = """
            {"code": "CAMPAIGN_NOT_FOUND", "message": "Campaign 9999 was not found"}""";

    private final GetCampaignMetricsUseCase campaignMetrics;
    private final ListCampaignMetricsUseCase allCampaignMetrics;
    private final BusinessBaselineUseCase baselines;

    public CampaignMetricsController(GetCampaignMetricsUseCase campaignMetrics,
                                     ListCampaignMetricsUseCase allCampaignMetrics,
                                     BusinessBaselineUseCase baselines) {
        this.campaignMetrics = campaignMetrics;
        this.allCampaignMetrics = allCampaignMetrics;
        this.baselines = baselines;
    }

    @Operation(summary = "Metrics of one campaign of the business in the token, day by day",
            description = "Impressions are the times an offer of the campaign appeared in the nearby search, clicks "
                    + "the times its detail was opened, reservations and redemptions come from Reservation. Every "
                    + "day of the period is in daily, with zeros when nothing happened. Without from and to the "
                    + "period is the validity of the campaign up to today; it covers at most 366 days. The rates are "
                    + "percentages with one decimal (clicks/impressions, reservations/clicks, redemptions/"
                    + "reservations, redemptions/clicks) and null when the base is 0. Impressions and clicks are "
                    + "written every few seconds, so the last ones can take that long to appear. baseline compares "
                    + "the redemptions with the customers the business declared it serves in the same number of "
                    + "days; it is null if the business did not declare one.")
    @ApiResponse(responseCode = "200", description = "Metrics of the period", content = @Content(examples = {
            @ExampleObject(name = "withBaseline", value = METRICS_EXAMPLE),
            @ExampleObject(name = "withoutActivityNorBaseline", value = METRICS_WITHOUT_BASELINE_EXAMPLE)}))
    @ApiResponse(responseCode = "400", description = "End before start, period longer than 366 days or a date "
            + "that is not yyyy-MM-dd", content = @Content(examples = {
                    @ExampleObject(name = "endBeforeStart", value = INVALID_PERIOD_EXAMPLE),
                    @ExampleObject(name = "invalidDate", value = """
                            {"code": "INVALID_REQUEST", "message": "from has an invalid value"}""")}))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or campaign of another business",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "Campaign does not exist",
            content = @Content(examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    @GetMapping("/{campaignId}/metrics")
    public CampaignMetricsResponse getMetrics(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Campaign id", example = "14") @PathVariable Long campaignId,
            @Parameter(description = "First day, Lima time", example = "2026-10-05")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day, included", example = "2026-10-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        var metrics = campaignMetrics.getMetrics(new GetCampaignMetricsQuery(campaignId, businessId, from, to));
        return CampaignMetricsAssembler.toResponse(metrics);
    }

    @Operation(summary = "Totals of every campaign of the business in the token",
            description = "A campaign without activity in the period comes with zeros, it is never left out. "
                    + "Without from and to the period is the last 30 days up to today.")
    @ApiResponse(responseCode = "200", description = "Campaigns with their totals",
            content = @Content(examples = @ExampleObject(value = SUMMARY_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Invalid period",
            content = @Content(examples = @ExampleObject(value = INVALID_PERIOD_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER",
            content = @Content(examples = @ExampleObject(value = OWNER_ONLY_EXAMPLE)))
    @GetMapping("/metrics")
    public CampaignMetricsSummaryResponse listMetrics(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "First day, Lima time", example = "2026-09-08")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day, included", example = "2026-10-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        return CampaignMetricsAssembler.toResponse(
                allCampaignMetrics.list(new ListCampaignMetricsQuery(businessId, from, to)));
    }

    @Operation(summary = "Baseline of the business in the token",
            description = "The customers per week and the average ticket the business had before GeoPS. 204 when "
                    + "it has not declared them yet.")
    @ApiResponse(responseCode = "200", description = "Declared baseline",
            content = @Content(examples = @ExampleObject(value = BASELINE_EXAMPLE)))
    @ApiResponse(responseCode = "204", description = "The business has not declared its baseline")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or without businessId",
            content = @Content(examples = @ExampleObject(value = OWNER_ONLY_EXAMPLE)))
    @GetMapping("/baseline")
    public ResponseEntity<BusinessBaselineResponse> getBaseline(@AuthenticationPrincipal Jwt jwt) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        return baselines.find(businessId)
                .map(baseline -> ResponseEntity.ok(BusinessBaselineResponse.of(baseline)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Declare or replace the baseline of the business in the token",
            description = "Records the customers per week and the average ticket with the current date. Declaring "
                    + "again replaces both numbers and the date. A business that just registered can declare it "
                    + "before creating its first campaign.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = DECLARE_BASELINE_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Baseline recorded",
            content = @Content(examples = @ExampleObject(value = BASELINE_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Customers out of 1-100000 or ticket not positive",
            content = @Content(examples = {
                    @ExampleObject(name = "invalidCustomers", value = """
                            {"code": "INVALID_REQUEST", "message": "customersPerWeek must be greater than or equal to 1"}"""),
                    @ExampleObject(name = "invalidTicket", value = """
                            {"code": "INVALID_REQUEST", "message": "averageTicket must be greater than or equal to 0.01"}""")}))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_BUSINESS_OWNER or without businessId",
            content = @Content(examples = @ExampleObject(value = OWNER_ONLY_EXAMPLE)))
    @PutMapping(value = "/baseline", consumes = APPLICATION_JSON_VALUE)
    public BusinessBaselineResponse declareBaseline(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeclareBaselineRequest request) {
        var businessId = AuthenticatedUser.from(jwt).requireBusinessId();
        return BusinessBaselineResponse.of(baselines.declare(request.toCommand(businessId)));
    }
}
