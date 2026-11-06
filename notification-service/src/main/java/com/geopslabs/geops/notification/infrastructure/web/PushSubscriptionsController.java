package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/push-subscriptions")
@Tag(name = "Push subscriptions")
@ApiResponse(responseCode = "401", description = "Valid token required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid token is required\"}")))
@ApiResponse(responseCode = "403", description = "Consumer role and own subscription required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"Este navegador ya está asociado a otra cuenta.\"}")))
public class PushSubscriptionsController {
    private final RegisterPushSubscriptionUseCase register;
    private final RemovePushSubscriptionUseCase remove;
    public PushSubscriptionsController(RegisterPushSubscriptionUseCase register, RemovePushSubscriptionUseCase remove) { this.register = register; this.remove = remove; }
    @Operation(summary = "Register an FCM token after the consumer grants browser permission")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{\"fcmToken\":\"browser-token\",\"userAgent\":\"Mozilla/5.0\"}")))
    @ApiResponse(responseCode = "201", description = "Subscription registered", content = @Content(examples = @ExampleObject(value = "{\"subscriptionId\":12,\"createdAt\":\"2026-10-27T14:11:00Z\"}")))
    @ApiResponse(responseCode = "200", description = "Existing subscription", content = @Content(examples = @ExampleObject(value = "{\"subscriptionId\":12,\"createdAt\":\"2026-10-27T14:11:00Z\"}")))
    @ApiResponse(responseCode = "400", description = "Invalid token or user agent", content = @Content(examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Revisa estos datos: fcmToken.\"}")))
    @ApiResponse(responseCode = "404", description = "Recipient copy not received", content = @Content(examples = @ExampleObject(value = "{\"code\":\"RECIPIENT_NOT_FOUND\",\"message\":\"Tu cuenta todavía no está lista para recibir avisos.\"}")))
    @PostMapping
    public ResponseEntity<Response> register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request request) {
        var result = register.register(AuthenticatedUser.from(jwt).consumerId(), request.fcmToken(), request.userAgent());
        return ResponseEntity.status(result.created() ? 201 : 200).body(new Response(result.subscription().id(), result.subscription().createdAt()));
    }
    @Operation(summary = "Remove a subscription owned by the authenticated consumer")
    @ApiResponse(responseCode = "204", description = "Subscription removed or absent")
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long subscriptionId) { remove.remove(AuthenticatedUser.from(jwt).consumerId(), subscriptionId); return ResponseEntity.noContent().build(); }
    public record Request(@NotBlank @Size(max = 500) String fcmToken, @Size(max = 255) String userAgent) {}
    public record Response(Long subscriptionId, Instant createdAt) {}
}
