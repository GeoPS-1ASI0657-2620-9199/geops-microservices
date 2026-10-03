package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.GetUserByEmailQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByIdQuery;
import com.geopslabs.geops.identity.application.usecases.UpdateUserCommand;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.application.usecases.UserCommandUseCase;
import com.geopslabs.geops.identity.infrastructure.web.resources.UserResource;
import com.geopslabs.geops.identity.infrastructure.web.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Users", description = "User identity operations and management")
@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
public class UserController {
    private final UserQueryUseCase userQueryService;
    private final UserCommandUseCase userCommandService;

    public UserController(UserQueryUseCase userQueryService, UserCommandUseCase userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResource> getById(
            @Parameter(description = "User unique identifier") @PathVariable Long id) {
        var query = new GetUserByIdQuery(id);
        var user = userQueryService.handle(query);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }

    @Operation(summary = "Get current authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResource> getMe(
            @Parameter(description = "Email of authenticated user")
            @RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var query = new GetUserByEmailQuery(email);
        var user = userQueryService.handle(query);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }

    @Operation(summary = "Update user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResource> updateUser(
            @Parameter(description = "User unique identifier") @PathVariable Long id,
            @RequestBody UserResource userResource) {
        var query = new GetUserByIdQuery(id);
        var existing = userQueryService.handle(query);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var cmd = new UpdateUserCommand(
            id,
            userResource.name(),
            userResource.email(),
            userResource.phone(),
            userResource.role()
        );

        var updatedOpt = userCommandService.handle(cmd);
        if (updatedOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var saved = updatedOpt.get();
        var resource = UserResourceFromEntityAssembler.toResourceFromEntity(saved);
        return ResponseEntity.ok(resource);
    }
}
