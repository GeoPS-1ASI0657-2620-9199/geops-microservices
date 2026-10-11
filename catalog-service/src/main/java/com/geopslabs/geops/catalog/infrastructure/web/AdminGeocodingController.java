package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.GeocodingRun;
import com.geopslabs.geops.catalog.application.usecases.GeocodingSummary;
import com.geopslabs.geops.catalog.application.usecases.RunGeocodingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @Operation(summary = "Start converting to coordinates the public source offers that wait for them",
            description = "Answers 202 with the run id at once; the run goes on in the background, one request per "
                    + "second to Nominatim. Each offer ends GEOCODED or in MANUAL_REVIEW; if Nominatim does not answer "
                    + "the run stops and the rest stay PENDING for the next run (stoppedByProvider). Offers already "
                    + "converted are never touched (US07). Progress and report: GET /api/v1/admin/geocoding-summary.")
    @ApiResponse(responseCode = "202", description = "Run started",
            content = @Content(examples = @ExampleObject(value = """
                    {"runId": "3f6c2d1e-8a4b-4c7d-9e2f-1a2b3c4d5e6f", "status": "RUNNING",
                     "startedAt": "2026-10-21T15:00:00Z", "finishedAt": null, "report": null}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @ApiResponse(responseCode = "409", description = "Another run is in progress",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "GEOCODING_RUN_IN_PROGRESS", "message": "Ya hay una conversión a coordenadas en curso. Inténtalo cuando termine."}""")))
    @PostMapping("/geocoding-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GeocodingRun start() {
        return geocoding.start();
    }

    @Operation(summary = "Count the public source offers by geocoding status and show the last run (E2-10)",
            description = "lastRun is the last run started in this replica: RUNNING, FINISHED with its report, "
                    + "SKIPPED (another replica held the lock) or FAILED; null if none since it started.")
    @ApiResponse(responseCode = "200", description = "Counts and last run",
            content = @Content(examples = @ExampleObject(value = """
                    {"geocoded": 140, "manualReview": 6, "pending": 0,
                     "lastRun": {"runId": "3f6c2d1e-8a4b-4c7d-9e2f-1a2b3c4d5e6f", "status": "FINISHED",
                       "startedAt": "2026-10-21T15:00:00Z", "finishedAt": "2026-10-21T15:00:13Z",
                       "report": {"processed": 12, "geocoded": 10, "manualReview": 2, "stillPending": 0,
                         "stoppedByProvider": false, "startedAt": "2026-10-21T15:00:00Z",
                         "finishedAt": "2026-10-21T15:00:13Z"}}}""")))
    @ApiResponse(responseCode = "403", description = "The token is not of an administrator")
    @GetMapping("/geocoding-summary")
    public GeocodingSummary summary() {
        return geocoding.summary();
    }
}
