package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.GetBusinessProfileUseCase;
import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucCommand;
import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Business profiles", description = "Profile of the business and verification of its RUC (US38)")
@SecurityRequirement(name = OpenApiConfiguration.SECURITY_SCHEME)
@RestController
@RequestMapping(value = "/api/v1/business-profiles", produces = APPLICATION_JSON_VALUE)
public class BusinessProfilesController {
    private final VerifyBusinessRucUseCase verifyBusinessRucUseCase;
    private final GetBusinessProfileUseCase getBusinessProfileUseCase;

    public BusinessProfilesController(VerifyBusinessRucUseCase verifyBusinessRucUseCase,
                                      GetBusinessProfileUseCase getBusinessProfileUseCase) {
        this.verifyBusinessRucUseCase = verifyBusinessRucUseCase;
        this.getBusinessProfileUseCase = getBusinessProfileUseCase;
    }

    @Operation(summary = "Verify the RUC of the business against SUNAT",
            description = "Checks the RUC of the profile in Identity's copy of the SUNAT reduced registry (active and "
                    + "habido) and geocodes the address with Nominatim. Every attempt is stored. The body is "
                    + "optional; its address replaces the declared one and is kept even if the attempt is not "
                    + "approved. When Nominatim does not answer the result is PENDING and it is retried later; the "
                    + "seal is never granted without geocoding.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(
            mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = RucVerificationRequest.class),
            examples = @ExampleObject(name = "correctedAddress", value = """
                    {"address": "Av. José Larco 345, Miraflores"}""")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attempt stored; result says what happened",
                    content = @Content(schema = @Schema(implementation = RucVerificationResponse.class), examples = {
                            @ExampleObject(name = "approved", value = """
                                    {"verificationId": 31, "result": "APPROVED", "reason": null,
                                     "message": "Tu comercio quedó verificado. El sello aparecerá junto a tus ofertas.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": "ACTIVO",
                                     "taxpayerCondition": "HABIDO", "verificationStatus": "VERIFIED",
                                     "checkedAt": "2026-10-21T15:04:00Z"}"""),
                            @ExampleObject(name = "rucNotFound", value = """
                                    {"verificationId": 32, "result": "REJECTED", "reason": "RUC_NOT_FOUND",
                                     "message": "No encontramos el RUC 20999999999 en el padrón de SUNAT de Lima y Callao. Revisa que sea el RUC de tu negocio; si está mal registrado, escríbenos para corregirlo.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": null,
                                     "taxpayerCondition": null, "verificationStatus": "REJECTED",
                                     "checkedAt": "2026-10-21T15:04:00Z"}"""),
                            @ExampleObject(name = "taxpayerNotActive", value = """
                                    {"verificationId": 33, "result": "REJECTED", "reason": "TAXPAYER_NOT_ACTIVE",
                                     "message": "SUNAT muestra tu RUC con estado \\"BAJA DE OFICIO\\". Regulariza el estado en SUNAT y vuelve a solicitar la verificación.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": "BAJA DE OFICIO",
                                     "taxpayerCondition": "HABIDO", "verificationStatus": "REJECTED",
                                     "checkedAt": "2026-10-21T15:04:00Z"}"""),
                            @ExampleObject(name = "taxpayerNotHabido", value = """
                                    {"verificationId": 34, "result": "REJECTED", "reason": "TAXPAYER_NOT_HABIDO",
                                     "message": "SUNAT muestra tu domicilio fiscal como \\"NO HABIDO\\". Actualízalo en SUNAT y vuelve a solicitar la verificación.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": "ACTIVO",
                                     "taxpayerCondition": "NO HABIDO", "verificationStatus": "REJECTED",
                                     "checkedAt": "2026-10-21T15:04:00Z"}"""),
                            @ExampleObject(name = "addressNotGeocodable", value = """
                                    {"verificationId": 35, "result": "REJECTED", "reason": "ADDRESS_NOT_GEOCODABLE",
                                     "message": "No ubicamos \\"Av. Larco 345, Miraflores\\" en el mapa de Lima y Callao. Corrige la dirección (vía, número y distrito) y vuelve a intentarlo; lo demás ya está validado.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": "ACTIVO",
                                     "taxpayerCondition": "HABIDO", "verificationStatus": "REJECTED",
                                     "checkedAt": "2026-10-21T15:04:00Z"}"""),
                            @ExampleObject(name = "geocoderUnavailable", value = """
                                    {"verificationId": 36, "result": "PENDING", "reason": "GEOCODER_UNAVAILABLE",
                                     "message": "El servicio de mapas no respondió. Tu verificación quedó pendiente y la reintentaremos en unos minutos; no tienes que hacer nada.",
                                     "address": "Av. Larco 345, Miraflores", "taxpayerStatus": "ACTIVO",
                                     "taxpayerCondition": "HABIDO", "verificationStatus": "PENDING",
                                     "checkedAt": "2026-10-21T15:04:00Z"}""")})),
            @ApiResponse(responseCode = "400", description = "address empty or longer than 255 characters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidRequest", value = """
                                    {"code": "INVALID_REQUEST", "message": "Revisa estos datos: address."}"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "The token is not of the owner of this business",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "forbidden", value = """
                                    {"code": "FORBIDDEN",
                                     "message": "Solo el dueño del negocio puede hacer esta operación."}"""))),
            @ApiResponse(responseCode = "404", description = "The business profile does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "The SUNAT registry has never been loaded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "registryEmpty", value = """
                                    {"code": "TAXPAYER_REGISTRY_EMPTY",
                                     "message": "Todavía no podemos verificar RUC. Inténtalo de nuevo en unos minutos."}""")))
    })
    @PostMapping("/{id}/ruc-verifications")
    @ResponseStatus(HttpStatus.CREATED)
    public RucVerificationResponse verifyRuc(@PathVariable("id") Long businessId,
                                             @Valid @RequestBody(required = false) RucVerificationRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        var requester = AuthenticatedUser.from(jwt).requireBusinessId();
        var address = request == null ? null : request.address();
        return RucVerificationResponse.from(
                verifyBusinessRucUseCase.verify(new VerifyBusinessRucCommand(businessId, requester, address)));
    }

    @Operation(summary = "Read the business profile with its verification status",
            description = "Returns the profile, its verification status and the last attempt, so the panel shows the "
                    + "real seal and what to correct.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile of the business",
                    content = @Content(schema = @Schema(implementation = BusinessProfileResponse.class),
                            examples = @ExampleObject(name = "verified", value = """
                                    {"id": 84, "businessName": "Restaurante Don Pepe", "businessType": "Restaurante",
                                     "ruc": "20123456789", "address": "Av. Larco 345, Miraflores",
                                     "verificationStatus": "VERIFIED",
                                     "lastVerification": {"result": "APPROVED", "reason": null,
                                      "message": "Tu comercio quedó verificado. El sello aparecerá junto a tus ofertas.",
                                      "checkedAt": "2026-10-21T15:04:00Z"}}"""))),
            @ApiResponse(responseCode = "403", description = "The token is not of the owner of this business",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The business profile does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public BusinessProfileResponse getProfile(@PathVariable("id") Long businessId, @AuthenticationPrincipal Jwt jwt) {
        var requester = AuthenticatedUser.from(jwt).requireBusinessId();
        return BusinessProfileResponse.from(getBusinessProfileUseCase.get(businessId, requester));
    }
}
