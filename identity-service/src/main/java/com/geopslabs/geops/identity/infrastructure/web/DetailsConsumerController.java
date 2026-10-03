package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.CreateDetailsConsumerCommand;
import com.geopslabs.geops.identity.application.usecases.UpdateDetailsConsumerCommand;
import com.geopslabs.geops.identity.application.usecases.GetDetailsConsumerByUserIdQuery;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerCommandUseCase;
import com.geopslabs.geops.identity.application.usecases.DetailsConsumerQueryUseCase;
import com.geopslabs.geops.identity.infrastructure.web.resources.CreateDetailsConsumerResource;
import com.geopslabs.geops.identity.infrastructure.web.resources.DetailsConsumerResource;
import com.geopslabs.geops.identity.infrastructure.web.transform.DetailsConsumerResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Consumer Details", description = "Consumer profile details operations and management")
@RestController
@RequestMapping(value = "/api/v1/users/{userId}/consumer-details", produces = APPLICATION_JSON_VALUE)
public class DetailsConsumerController {
    private final DetailsConsumerQueryUseCase detailsConsumerQueryService;
    private final DetailsConsumerCommandUseCase detailsConsumerCommandService;

    public DetailsConsumerController(DetailsConsumerQueryUseCase detailsConsumerQueryService,
                                    DetailsConsumerCommandUseCase detailsConsumerCommandService) {
        this.detailsConsumerQueryService = detailsConsumerQueryService;
        this.detailsConsumerCommandService = detailsConsumerCommandService;
    }

    @Operation(summary = "Get consumer details by user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer details found"),
        @ApiResponse(responseCode = "404", description = "Consumer details not found"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping
    public ResponseEntity<DetailsConsumerResource> getByUserId(
            @Parameter(description = "User unique identifier") @PathVariable Long userId) {
        var query = new GetDetailsConsumerByUserIdQuery(userId);
        var detailsConsumer = detailsConsumerQueryService.handle(query);

        if (detailsConsumer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var resource = DetailsConsumerResourceFromEntityAssembler.toResourceFromEntity(detailsConsumer.get());
        return ResponseEntity.ok(resource);
    }

    @Operation(summary = "Create consumer details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Consumer details created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or consumer details already exist"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DetailsConsumerResource> createConsumerDetails(
            @Parameter(description = "User unique identifier") @PathVariable Long userId,
            @RequestBody CreateDetailsConsumerResource resource) {
        var command = new CreateDetailsConsumerCommand(
            userId,
            resource.categoriasFavoritas(),
            resource.permisoUbicacion(),
            resource.direccionCasa(),
            resource.direccionTrabajo(),
            resource.direccionUniversidad()
        );

        var createdOpt = detailsConsumerCommandService.handle(command);
        if (createdOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var saved = createdOpt.get();
        var responseResource = DetailsConsumerResourceFromEntityAssembler.toResourceFromEntity(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseResource);
    }

    @Operation(summary = "Update consumer details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer details updated"),
        @ApiResponse(responseCode = "404", description = "Consumer details not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<DetailsConsumerResource> updateConsumerDetails(
            @Parameter(description = "User unique identifier") @PathVariable Long userId,
            @RequestBody CreateDetailsConsumerResource resource) {
        var command = new UpdateDetailsConsumerCommand(
            userId,
            resource.categoriasFavoritas(),
            resource.permisoUbicacion(),
            resource.direccionCasa(),
            resource.direccionTrabajo(),
            resource.direccionUniversidad()
        );

        var updatedOpt = detailsConsumerCommandService.handle(command);
        if (updatedOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var saved = updatedOpt.get();
        var responseResource = DetailsConsumerResourceFromEntityAssembler.toResourceFromEntity(saved);
        return ResponseEntity.ok(responseResource);
    }
}
