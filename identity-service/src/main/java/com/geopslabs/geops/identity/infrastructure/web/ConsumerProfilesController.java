package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.ChangeSearchRadiusUseCase;
import com.geopslabs.geops.identity.application.usecases.GetConsumerProfileUseCase;
import com.geopslabs.geops.identity.configuration.OpenApiConfiguration;
import com.geopslabs.geops.identity.shared.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Consumer profiles", description = "Preferences of the consumer with a session (US46)")
@SecurityRequirement(name = OpenApiConfiguration.SECURITY_SCHEME)
@RestController
@RequestMapping(value = "/api/v1/consumer-profiles/me", produces = APPLICATION_JSON_VALUE)
public class ConsumerProfilesController {
    private final ChangeSearchRadiusUseCase changeSearchRadiusUseCase;
    private final GetConsumerProfileUseCase getConsumerProfileUseCase;

    public ConsumerProfilesController(ChangeSearchRadiusUseCase changeSearchRadiusUseCase,
                                      GetConsumerProfileUseCase getConsumerProfileUseCase) {
        this.changeSearchRadiusUseCase = changeSearchRadiusUseCase;
        this.getConsumerProfileUseCase = getConsumerProfileUseCase;
    }

    @Operation(summary = "Save the walking radius of the search",
            description = "Keeps the radius chosen by the consumer, from 5 to 20 minutes, and publishes "
                    + "ConsumerPreferencesChanged when it changes.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = SearchRadiusRequest.class),
            examples = @ExampleObject(name = "fifteenMinutes", value = """
                    {"radiusMinutes": 15}""")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Radius saved",
                    content = @Content(schema = @Schema(implementation = ConsumerProfileResponse.class),
                            examples = @ExampleObject(name = "saved", value = """
                                    {"consumerId": 41, "searchRadiusMinutes": 15, "locationPermission": false,
                                     "defaultDistrict": null}"""))),
            @ApiResponse(responseCode = "400", description = "Radius missing or outside 5 to 20 minutes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "outOfRange", value = """
                                    {"code": "RADIUS_OUT_OF_RANGE",
                                     "message": "Elige un radio entre 5 y 20 minutos a pie."}"""))),
            @ApiResponse(responseCode = "403", description = "The token is not of a consumer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/search-radius", consumes = APPLICATION_JSON_VALUE)
    public ConsumerProfileResponse changeSearchRadius(@Valid @RequestBody SearchRadiusRequest request,
                                                      @AuthenticationPrincipal Jwt jwt) {
        var consumerId = AuthenticatedUser.from(jwt).consumerId();
        return ConsumerProfileResponse.from(
                changeSearchRadiusUseCase.changeSearchRadius(consumerId, request.radiusMinutes()));
    }

    @Operation(summary = "Read the consumer profile", description = "Returns the saved walking radius.")
    @ApiResponse(responseCode = "200", description = "Profile of the consumer",
            content = @Content(schema = @Schema(implementation = ConsumerProfileResponse.class)))
    @GetMapping
    public ConsumerProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ConsumerProfileResponse.from(getConsumerProfileUseCase.get(AuthenticatedUser.from(jwt).consumerId()));
    }
}
