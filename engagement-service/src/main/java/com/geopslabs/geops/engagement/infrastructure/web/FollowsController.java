package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.models.commands.*;
import com.geopslabs.geops.engagement.domain.models.queries.GetFollowedBusinessesQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/follows")
@Tag(name = "Follows", description = "Businesses followed by the authenticated consumer")
@ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid token is required\"}")))
@ApiResponse(responseCode = "403", description = "Consumer role required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"Consumer role required\"}")))
public class FollowsController {
    private static final String EXAMPLE = """
            {"businessId":84,"businessName":"Restaurante Don Pepe","followedAt":"2026-10-27T16:00:00Z",
             "activeOffersCount":1,"activeOffers":[{"offerId":1052,"title":"2x1","validTo":"2026-11-15"}]}""";
    private final FollowBusinessUseCase follow;
    private final UnfollowBusinessUseCase unfollow;
    private final ListFollowedBusinessesUseCase list;

    public FollowsController(FollowBusinessUseCase follow, UnfollowBusinessUseCase unfollow, ListFollowedBusinessesUseCase list) {
        this.follow = follow;
        this.unfollow = unfollow;
        this.list = list;
    }

    @Operation(summary = "Follow a business", description = "Following twice returns the existing follow without publishing another event.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{\"businessId\":84}")))
    @ApiResponse(responseCode = "201", description = "Business followed", content = @Content(examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Already followed", content = @Content(examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Invalid business id", content = @Content(examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Revisa estos datos: businessId.\"}")))
    @ApiResponse(responseCode = "404", description = "No local business copy", content = @Content(examples = @ExampleObject(value = "{\"code\":\"BUSINESS_NOT_FOUND\",\"message\":\"No encontramos ese comercio.\"}")))
    @PostMapping
    public ResponseEntity<FollowedBusinessView> follow(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FollowRequest request) {
        var result = follow.follow(new FollowBusinessCommand(AuthenticatedUser.from(jwt).consumerId(), request.businessId()));
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.business());
    }

    @Operation(summary = "List followed businesses and their currently published offers")
    @ApiResponse(responseCode = "200", description = "Followed businesses", content = @Content(examples = @ExampleObject(value = "[" + EXAMPLE + "]")))
    @GetMapping
    public List<FollowedBusinessView> list(@AuthenticationPrincipal Jwt jwt) {
        return list.list(new GetFollowedBusinessesQuery(AuthenticatedUser.from(jwt).consumerId()));
    }

    @Operation(summary = "Unfollow a business", description = "Repeated removal returns 204 without publishing another event.")
    @ApiResponse(responseCode = "204", description = "Business unfollowed")
    @DeleteMapping("/{businessId}")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long businessId) {
        unfollow.unfollow(new UnfollowBusinessCommand(AuthenticatedUser.from(jwt).consumerId(), businessId));
        return ResponseEntity.noContent().build();
    }
}
