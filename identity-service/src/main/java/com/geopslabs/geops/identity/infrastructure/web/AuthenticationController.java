package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.GetUserByEmailQuery;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.geopslabs.geops.identity.infrastructure.web.resources.AuthenticationResource;
import com.geopslabs.geops.identity.infrastructure.web.resources.SignInResource;
import com.geopslabs.geops.identity.shared.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Authentication", description = "User registration and login")
@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class AuthenticationController {
    private final RegisterUserUseCase registerUserUseCase;
    private final UserQueryUseCase userQueryService;
    private final PasswordHasherPort hashingService;
    private final TokenIssuerPort tokenService;

    public AuthenticationController(RegisterUserUseCase registerUserUseCase,
                                    UserQueryUseCase userQueryService,
                                    PasswordHasherPort hashingService,
                                    TokenIssuerPort tokenService) {
        this.registerUserUseCase = registerUserUseCase;
        this.userQueryService = userQueryService;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Register a consumer or a business owner with its profile",
            description = "Creates the account and its consumer profile. It does not log the user in.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = RegisterUserRequest.class),
            examples = @ExampleObject(name = "consumer", value = """
                    {"role": "CONSUMER", "fullName": "Lucía Fernández Ríos",
                     "email": "lucia.fernandez@ejemplo.pe", "phone": "987123456",
                     "password": "Ofertas#2026"}""")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account and consumer profile created",
                    content = @Content(schema = @Schema(implementation = RegisteredUserResponse.class),
                            examples = @ExampleObject(name = "consumerCreated", value = """
                                    {"userId": 41, "fullName": "Lucía Fernández Ríos",
                                     "email": "lucia.fernandez@ejemplo.pe", "role": "CONSUMER",
                                     "consumerProfileId": 15}"""))),
            @ApiResponse(responseCode = "400", description = "Invalid data or role not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "invalidRequest", value = """
                                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: password, phone."}"""),
                            @ExampleObject(name = "roleNotAllowed", value = """
                                    {"code": "ROLE_NOT_ALLOWED",
                                     "message": "Ese tipo de cuenta no se puede crear desde el registro."}""")})),
            @ApiResponse(responseCode = "409", description = "Email or phone already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "emailAlreadyRegistered", value = """
                                    {"code": "EMAIL_ALREADY_REGISTERED",
                                     "message": "Ese correo ya tiene una cuenta. Inicia sesión o usa otro correo."}"""),
                            @ExampleObject(name = "phoneAlreadyRegistered", value = """
                                    {"code": "PHONE_ALREADY_REGISTERED",
                                     "message": "Ese teléfono ya tiene una cuenta. Inicia sesión o usa otro teléfono."}""")}))
    })
    @PostMapping(value = "/api/v1/auth/register", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        var registered = registerUserUseCase.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisteredUserResponse.from(registered));
    }

    @Operation(summary = "Authenticate a user", description = "Validates user credentials and returns user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User authenticated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/api/v1/authentication/sign-in")
    public ResponseEntity<AuthenticationResource> signIn(@RequestBody SignInResource resource) {
        if (resource.email() == null || resource.email().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (resource.password() == null || resource.password().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var query = new GetUserByEmailQuery(resource.email());
        var userOptional = userQueryService.handle(query);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = userOptional.get();

        if (!hashingService.matches(resource.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var token = tokenService.generateToken(user.getEmail());

        var authResource = new AuthenticationResource(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name(),
            token,
            "User authenticated successfully"
        );

        return ResponseEntity.ok(authResource);
    }
}
