package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.UpdateNotificationPreferencesUseCase;
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

@Tag(name = "Notification preferences", description = "Channels and daily limit of the notices of a consumer")
@RestController
@RequestMapping(value = NotificationPreferencesController.PREFERENCES_PATH, produces = APPLICATION_JSON_VALUE)
public class NotificationPreferencesController {
    static final String PREFERENCES_PATH = "/api/v1/notification-preferences";

    private static final String UNAUTHORIZED_EXAMPLE = """
            {"code": "UNAUTHORIZED", "message": "A valid token is required"}""";
    private static final String FORBIDDEN_EXAMPLE = """
            {"code": "FORBIDDEN", "message": "Your account is not allowed to perform this operation"}""";

    private final UpdateNotificationPreferencesUseCase updatePreferences;

    public NotificationPreferencesController(UpdateNotificationPreferencesUseCase updatePreferences) {
        this.updatePreferences = updatePreferences;
    }

    @Operation(summary = "Create or replace the notification preferences of the consumer",
            description = "Turns each channel on or off and sets the daily limit shared by all channels.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "emailOnly", value = """
                    {"pushEnabled": false, "emailEnabled": true, "dailyLimit": 3}""")))
    @ApiResponse(responseCode = "200", description = "Preferences stored",
            content = @Content(examples = @ExampleObject(value = """
                    {"consumerId": 1, "pushEnabled": false, "emailEnabled": true, "dailyLimit": 3,
                     "updatedAt": "2026-10-08T13:05:00Z"}""")))
    @ApiResponse(responseCode = "400", description = "Missing channel or daily limit outside 1 to 10",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: dailyLimit."}""")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "403", description = "Token without ROLE_CONSUMER",
            content = @Content(examples = @ExampleObject(value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "The recipient copy has not arrived from Identity",
            content = @Content(examples = @ExampleObject(value = """
                    {"code": "RECIPIENT_NOT_FOUND", "message": "Tu cuenta todavía no está lista para recibir avisos."}""")))
    @PutMapping(consumes = APPLICATION_JSON_VALUE)
    public PreferencesResponse updatePreferences(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        return PreferencesResponse.from(updatePreferences.update(request.toCommand(consumerId)));
    }
}
