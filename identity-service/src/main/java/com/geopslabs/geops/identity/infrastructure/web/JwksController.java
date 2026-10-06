package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.GetPublicKeysUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Public keys", description = "Keys that validate the tokens issued by Identity")
@RestController
public class JwksController {
    private final GetPublicKeysUseCase getPublicKeysUseCase;

    public JwksController(GetPublicKeysUseCase getPublicKeysUseCase) {
        this.getPublicKeysUseCase = getPublicKeysUseCase;
    }

    @Operation(summary = "Publish the public keys that validate Identity tokens",
            description = "Returns the RSA public keys as a JWK Set. It never includes private key material.")
    @ApiResponse(responseCode = "200", description = "JWK Set with the current signing key",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "jwkSet", value = """
                    {"keys": [{"kty": "RSA", "use": "sig", "alg": "RS256", "kid": "identity-2026-10",
                     "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                     "e": "AQAB"}]}""")))
    @GetMapping(value = "/.well-known/jwks.json", produces = APPLICATION_JSON_VALUE)
    public Map<String, Object> publicKeys() {
        return getPublicKeysUseCase.publicKeys();
    }
}
