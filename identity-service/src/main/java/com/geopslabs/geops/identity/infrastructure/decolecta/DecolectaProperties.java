package com.geopslabs.geops.identity.infrastructure.decolecta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

/**
 * geops.decolecta.*: where the online RUC lookup answers, the token (only from the environment, never in the
 * repository), the connect and read timeout, and how long an answer is reused so the retries of a pending
 * verification do not spend the monthly quota.
 */
@ConfigurationProperties(prefix = "geops.decolecta")
public record DecolectaProperties(@DefaultValue("https://api.decolecta.com") URI baseUrl,
                                  @DefaultValue("") String token,
                                  @DefaultValue("3s") Duration timeout,
                                  @DefaultValue("24h") Duration cacheTtl) {

    boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
