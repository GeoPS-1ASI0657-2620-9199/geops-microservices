package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GeocodingRunReport;
import com.geopslabs.geops.catalog.application.usecases.GeocodingSummary;
import com.geopslabs.geops.catalog.application.usecases.RunGeocodingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Administration", description = "Operations of the GeoPS administrator (ROLE_ADMIN)")
@RestController
@RequestMapping(value = "/api/v1/admin", produces = APPLICATION_JSON_VALUE)
public class AdminGeocodingController {
    private final RunGeocodingUseCase geocoding;

    public AdminGeocodingController(RunGeocodingUseCase geocoding) {
        this.geocoding = geocoding;
    }

    @Operation(summary = "Convert to coordinates the public source offers that wait for them",
            description = "Takes the offers in PENDING, one request per second to Nominatim. Each one ends GEOCODED "
                    + "or in MANUAL_REVIEW; if Nominatim does not answer the run stops and the rest stay PENDING for "
                    + "the next run (stoppedByProvider). Offers already converted are never touched (US07).")
    @ApiResponse(responseCode = "200", description = "Report of the run",
            content = @Content(examples = @ExampleObject(value = """
                    {"processed": 12, "geocoded": 10, "manualReview": 2, "stillPending": 0,
                     "stoppedByProvider": false, "startedAt": "2026-10-21T15:00:00Z",
                     "finishedAt": "2026-10-21T15:00:13Z"}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @ApiResponse(responseCode = "409", description = "Another run is in progress",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "GEOCODING_RUN_IN_PROGRESS", "message": "Ya hay una conversión a coordenadas en curso. Inténtalo cuando termine."}""")))
    @PostMapping("/geocoding-runs")
    public GeocodingRunReport run() {
        return geocoding.run();
    }

    @Operation(summary = "Count the public source offers by geocoding status (E2-10)")
    @ApiResponse(responseCode = "200", description = "Counts",
            content = @Content(examples = @ExampleObject(value = """
                    {"geocoded": 140, "manualReview": 6, "pending": 0}""")))
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @GetMapping("/geocoding-summary")
    public GeocodingSummary summary() {
        return geocoding.summary();
    }
}
