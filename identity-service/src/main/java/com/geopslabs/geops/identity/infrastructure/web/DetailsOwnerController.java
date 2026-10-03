package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.CreateDetailsOwnerCommand;
import com.geopslabs.geops.identity.application.usecases.UpdateDetailsOwnerCommand;
import com.geopslabs.geops.identity.application.usecases.GetDetailsOwnerByUserIdQuery;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.DetailsOwnerQueryUseCase;
import com.geopslabs.geops.identity.infrastructure.web.resources.CreateDetailsOwnerResource;
import com.geopslabs.geops.identity.infrastructure.web.resources.DetailsOwnerResource;
import com.geopslabs.geops.identity.infrastructure.web.transform.DetailsOwnerResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Owner Details", description = "Owner/business profile details operations and management")
@RestController
@RequestMapping(value = "/api/v1/users/{userId}/owner-details", produces = APPLICATION_JSON_VALUE)
public class DetailsOwnerController {
    private final DetailsOwnerQueryUseCase detailsOwnerQueryService;
    private final DetailsOwnerCommandUseCase detailsOwnerCommandService;

    public DetailsOwnerController(DetailsOwnerQueryUseCase detailsOwnerQueryService,
                                 DetailsOwnerCommandUseCase detailsOwnerCommandService) {
        this.detailsOwnerQueryService = detailsOwnerQueryService;
        this.detailsOwnerCommandService = detailsOwnerCommandService;
    }

    @Operation(summary = "Get owner details by user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Owner details found"),
        @ApiResponse(responseCode = "404", description = "Owner details not found"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping
    public ResponseEntity<DetailsOwnerResource> getByUserId(
            @Parameter(description = "User unique identifier") @PathVariable Long userId) {
        var query = new GetDetailsOwnerByUserIdQuery(userId);
        var detailsOwner = detailsOwnerQueryService.handle(query);

        if (detailsOwner.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var resource = DetailsOwnerResourceFromEntityAssembler.toResourceFromEntity(detailsOwner.get());
        return ResponseEntity.ok(resource);
    }

    @Operation(summary = "Create owner details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Owner details created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or owner details already exist"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DetailsOwnerResource> createOwnerDetails(
            @Parameter(description = "User unique identifier") @PathVariable Long userId,
            @RequestBody CreateDetailsOwnerResource resource) {
        var command = new CreateDetailsOwnerCommand(
            userId,
            resource.businessName(),
            resource.businessType(),
            resource.ruc(),
            resource.address(),
            resource.openingHours()
        );

        var createdOpt = detailsOwnerCommandService.handle(command);
        if (createdOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var saved = createdOpt.get();
        var responseResource = DetailsOwnerResourceFromEntityAssembler.toResourceFromEntity(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseResource);
    }

    @Operation(summary = "Update owner details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Owner details updated"),
        @ApiResponse(responseCode = "404", description = "Owner details not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DetailsOwnerResource> updateOwnerDetails(
            @Parameter(description = "User unique identifier") @PathVariable Long userId,
            @RequestBody CreateDetailsOwnerResource resource) {
        var command = new UpdateDetailsOwnerCommand(
            userId,
            resource.businessName(),
            resource.businessType(),
            resource.address(),
            resource.openingHours()
        );

        var updatedOpt = detailsOwnerCommandService.handle(command);
        if (updatedOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var saved = updatedOpt.get();
        var responseResource = DetailsOwnerResourceFromEntityAssembler.toResourceFromEntity(saved);
        return ResponseEntity.ok(responseResource);
    }
}
