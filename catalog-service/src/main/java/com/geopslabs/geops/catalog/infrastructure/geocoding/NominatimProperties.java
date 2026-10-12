package com.geopslabs.geops.catalog.infrastructure.geocoding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

/**
 * geops.nominatim.*: where Nominatim answers, the contact sent in the User-Agent, the connect and read timeout
 * (2 s, S2-03) and the minimum time between two requests (1 s, E2-15).
 */
@ConfigurationProperties(prefix = "geops.nominatim")
public record NominatimProperties(@DefaultValue("https://nominatim.openstreetmap.org") URI baseUrl,
                                  @DefaultValue("") String contactEmail,
                                  @DefaultValue("2s") Duration timeout,
                                  @DefaultValue("1s") Duration minInterval) {

    private static final String USER_AGENT = "GeoPS/1.0";
    private static final String USER_AGENT_WITH_CONTACT = USER_AGENT + " (%s)";

    String userAgent() {
        return contactEmail == null || contactEmail.isBlank()
                ? USER_AGENT : USER_AGENT_WITH_CONTACT.formatted(contactEmail.strip());
    }
}
