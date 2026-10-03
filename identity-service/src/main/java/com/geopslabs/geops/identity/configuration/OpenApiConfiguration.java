package com.geopslabs.geops.identity.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    private static final String TITLE = "GeoPS Identity API";
    private static final String VERSION = "0.1.0";
    private static final String DESCRIPTION = "Registration, login and public keys of GeoPS users";

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI().info(new Info().title(TITLE).version(VERSION).description(DESCRIPTION));
    }
}
