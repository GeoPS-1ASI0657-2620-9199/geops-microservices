package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.UnsubscribeEmailUseCase;
import com.geopslabs.geops.notification.domain.models.EmailUnsubscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email-unsubscriptions")
@Tag(name = "Email unsubscriptions")
public class EmailUnsubscriptionsController {
    private final UnsubscribeEmailUseCase unsubscribe;
    public EmailUnsubscriptionsController(UnsubscribeEmailUseCase unsubscribe) { this.unsubscribe = unsubscribe; }
    @Operation(summary = "Stop one type of offer email using its signed link", description = "Requires explicit POST confirmation. Keeps the account and the global email preference active.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{\"token\":\"2001.NEARBY_OFFER.signed-token\"}")))
    @ApiResponse(responseCode = "200", description = "Unsubscribed", content = @Content(examples = @ExampleObject(value = "{\"notificationType\":\"NEARBY_OFFER\",\"unsubscribedAt\":\"2026-10-29T08:00:00Z\"}")))
    @ApiResponse(responseCode = "400", description = "Invalid token", content = @Content(examples = @ExampleObject(value = "{\"code\":\"INVALID_UNSUBSCRIBE_TOKEN\",\"message\":\"El enlace de baja no es válido.\"}")))
    @PostMapping
    public Response unsubscribe(@Valid @RequestBody Request request) {
        var result = unsubscribe.unsubscribe(request.token());
        return new Response(result.notificationType().name(), result.unsubscribedAt());
    }
    public record Request(@NotBlank @Size(max = 64) String token) {}
    public record Response(String notificationType, java.time.Instant unsubscribedAt) {}
}
