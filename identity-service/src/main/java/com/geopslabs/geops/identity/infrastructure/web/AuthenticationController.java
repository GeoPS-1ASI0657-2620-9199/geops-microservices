package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.LogInUseCase;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
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
@RequestMapping(value = "/api/v1/auth", produces = APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class AuthenticationController {
    private final RegisterUserUseCase registerUserUseCase;
    private final LogInUseCase logInUseCase;

    public AuthenticationController(RegisterUserUseCase registerUserUseCase, LogInUseCase logInUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.logInUseCase = logInUseCase;
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
    @PostMapping(value = "/register", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        var registered = registerUserUseCase.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisteredUserResponse.from(registered));
    }

    @Operation(summary = "Check credentials and issue an RS256 token",
            description = "Any failure answers the same 401 so the response never says which data was wrong.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = LogInRequest.class),
            examples = @ExampleObject(name = "consumer", value = """
                    {"email": "lucia.fernandez@ejemplo.pe", "password": "Ofertas#2026"}""")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valid credentials; signed token issued",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(name = "consumerToken", value = """
                                    {"accessToken": "eyJraWQiOiJpZGVudGl0eS0yMDI2LTEwIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI0MSIsInJvbGUiOiJDT05TVU1FUiIsImNvbnN1bWVySWQiOjQxfQ.c2lnbmF0dXJh",
                                     "tokenType": "Bearer", "expiresIn": 3600, "userId": 41,
                                     "role": "CONSUMER", "consumerId": 41}"""))),
            @ApiResponse(responseCode = "400", description = "Missing or malformed email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = """
                                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: email."}"""))),
            @ApiResponse(responseCode = "401", description = "Unknown email, wrong password or locked account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidCredentials", value = """
                                    {"code": "INVALID_CREDENTIALS",
                                     "message": "No se pudo iniciar sesión con esos datos."}""")))
    })
    @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE)
    public TokenResponse logIn(@Valid @RequestBody LogInRequest request) {
        return TokenResponse.from(logInUseCase.logIn(request.toCommand()));
    }
}
