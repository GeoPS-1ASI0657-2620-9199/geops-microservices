package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.RecordLastKnownLocationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Last known location", description = "Where the consumer was last seen, for nearby notices")
@RestController
@RequestMapping(value = LastKnownLocationsController.LOCATIONS_PATH, produces = APPLICATION_JSON_VALUE)
public class LastKnownLocationsController {
    static final String LOCATIONS_PATH = "/api/v1/locations";

    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";

    private final RecordLastKnownLocationUseCase recordLastKnownLocation;

    public LastKnownLocationsController(RecordLastKnownLocationUseCase recordLastKnownLocation) {
        this.recordLastKnownLocation = recordLastKnownLocation;
    }

    @Operation(summary = "Record the last known location of the consumer with its capture time",
            description = "Keeps the newest reading. The location is fresh for nearby notices while it is less "
                    + "than 60 minutes old.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "location", value = """
                    {"latitude": -12.1211, "longitude": -77.0297, "accuracyMeters": 25,
                     "capturedAt": "2026-10-08T13:04:30Z"}""")))
    @ApiResponse(responseCode = "200", description = "Location stored or newer one kept",
            content = @Content(examples = @ExampleObject(value = """
                    {"consumerId": 1, "latitude": -12.1211, "longitude": -77.0297, "accuracyMeters": 25,
                     "radiusMeters": 800, "capturedAt": "2026-10-08T13:04:30Z", "fresh": true}""")))
    @ApiResponse(responseCode = "400", description = "Coordinates out of range or capture time in the future",
            content = @Content(examples = {
                    @ExampleObject(name = "invalidCaptureTime", value = """
                            {"code": "INVALID_CAPTURE_TIME", "message": "La hora de captura no puede estar en el futuro."}"""),
                    @ExampleObject(name = "invalidCoordinates", value = """
                            {"code": "INVALID_REQUEST", "message": "Revisa estos datos: latitude."}""")}))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "The recipient copy has not arrived from Identity",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RECIPIENT_NOT_FOUND", "message": "Tu cuenta todavía no está lista para recibir avisos."}""")))
    @PutMapping(value = "/last", consumes = APPLICATION_JSON_VALUE)
    public LastKnownLocationResponse recordLastLocation(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordLocationRequest request) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        return LastKnownLocationResponse.from(recordLastKnownLocation.record(request.toCommand(consumerId)));
    }
}
