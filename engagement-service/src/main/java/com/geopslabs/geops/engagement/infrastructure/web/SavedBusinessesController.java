package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/saved-businesses")
@Tag(name = "Saved businesses")
@ApiResponse(responseCode = "401", description = "Valid token required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"A valid token is required\"}")))
@ApiResponse(responseCode = "403", description = "Consumer role required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"Consumer role required\"}")))
public class SavedBusinessesController {
    private static final String EXAMPLE = "{\"id\":1,\"businessId\":84,\"businessName\":\"Don Pepe\",\"savedAt\":\"2026-10-27T16:00:00Z\"}";
    private final SaveBusinessUseCase save;
    private final ListSavedBusinessesUseCase list;
    private final RemoveSavedBusinessUseCase remove;
    public SavedBusinessesController(SaveBusinessUseCase save, ListSavedBusinessesUseCase list, RemoveSavedBusinessUseCase remove) { this.save = save; this.list = list; this.remove = remove; }

    @Operation(summary = "Save a business for later")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{\"businessId\":84}")))
    @ApiResponse(responseCode = "201", description = "Business saved", content = @Content(examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "Already saved", content = @Content(examples = @ExampleObject(value = EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Positive businessId required", content = @Content(examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"Revisa estos datos: businessId.\"}")))
    @ApiResponse(responseCode = "404", description = "Business copy missing", content = @Content(examples = @ExampleObject(value = "{\"code\":\"BUSINESS_NOT_FOUND\",\"message\":\"No encontramos ese comercio.\"}")))
    @PostMapping
    public ResponseEntity<SavedBusiness> save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request request) {
        var result = save.save(AuthenticatedUser.from(jwt).consumerId(), request.businessId());
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.business());
    }

    @Operation(summary = "List the saved businesses of the consumer")
    @ApiResponse(responseCode = "200", description = "Saved businesses", content = @Content(examples = @ExampleObject(value = "[" + EXAMPLE + "]")))
    @GetMapping
    public List<SavedBusiness> list(@AuthenticationPrincipal Jwt jwt) { return list.list(AuthenticatedUser.from(jwt).consumerId()); }

    @Operation(summary = "Remove a saved business")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "404", description = "Not saved", content = @Content(examples = @ExampleObject(value = "{\"code\":\"SAVED_BUSINESS_NOT_FOUND\",\"message\":\"No tienes guardado ese comercio.\"}")))
    @DeleteMapping("/{businessId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long businessId) { remove.remove(AuthenticatedUser.from(jwt).consumerId(), businessId); return ResponseEntity.noContent().build(); }

    public record Request(@NotNull @Positive Long businessId) {}
}
