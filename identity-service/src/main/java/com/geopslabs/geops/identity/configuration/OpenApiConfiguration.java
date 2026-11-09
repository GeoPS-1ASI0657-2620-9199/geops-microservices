package com.geopslabs.geops.identity.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registration, login and the JWKS are public; the profile controllers declare the bearer scheme themselves.
 */
@Configuration
public class OpenApiConfiguration {
    public static final String SECURITY_SCHEME = "bearerAuth";
    private static final String TITLE = "GeoPS Identity API";
    private static final String VERSION = "0.2.0";
    private static final String DESCRIPTION = "Registration, login, public keys and profiles of GeoPS users";
    private static final String BEARER = "bearer";
    private static final String TOKEN_FORMAT = "JWT";

    @Bean
    public OpenAPI identityOpenApi() {
        var bearerScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme(BEARER)
                .bearerFormat(TOKEN_FORMAT);
        return new OpenAPI()
                .info(new Info().title(TITLE).version(VERSION).description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, bearerScheme));
    }
}
