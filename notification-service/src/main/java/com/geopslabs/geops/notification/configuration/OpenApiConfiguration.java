package com.geopslabs.geops.notification.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    private static final String TITLE = "GeoPS Notification API";
    private static final String VERSION = "0.1.0";
    private static final String DESCRIPTION = "Notification preferences and last known location of the consumer";
    private static final String SECURITY_SCHEME = "bearerAuth";
    private static final String BEARER = "bearer";
    private static final String TOKEN_FORMAT = "JWT";

    @Bean
    public OpenAPI notificationOpenApi() {
        var bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER)
                .bearerFormat(TOKEN_FORMAT);
        return new OpenAPI()
                .info(new Info().title(TITLE).version(VERSION).description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
