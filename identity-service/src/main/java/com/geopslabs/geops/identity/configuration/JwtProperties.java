package com.geopslabs.geops.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("geops.security.jwt")
public record JwtProperties(@NotBlank String privateKeyPath, @NotBlank String publicKeyPath, @NotBlank String issuer,
                            @NotBlank String audience) {
}
