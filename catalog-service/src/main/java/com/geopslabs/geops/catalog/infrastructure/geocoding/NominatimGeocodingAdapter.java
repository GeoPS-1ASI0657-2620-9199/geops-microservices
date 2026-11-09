package com.geopslabs.geops.catalog.infrastructure.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingResult;
import com.geopslabs.geops.catalog.domain.ports.GeocodingPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Map;

/**
 * Nominatim behind GeocodingPort (S2-03, E2-14, E2-15). Each request waits at most 2 s to connect and 2 s to read,
 * is limited to Peru and spaced one second from the previous one. A point outside Lima Metropolitana and Callao
 * counts as not found. Errors, timeouts and the open circuit breaker all answer UNAVAILABLE, so the caller
 * leaves the offer PENDING; "no result" is a valid answer and does not count as a failure.
 */
public class NominatimGeocodingAdapter implements GeocodingPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(NominatimGeocodingAdapter.class);
    private static final String SEARCH_PATH = "/search";
    private static final String CITY_SUFFIX = ", Lima, Perú";
    private static final String QUERY_VARIABLE = "{q}";
    private static final String QUERY_KEY = "q";
    private static final String FORMAT = "jsonv2";
    private static final int ONE_RESULT = 1;
    private static final String PERU = "pe";
    /** Lima Metropolitana and Callao, from Ancón to Pucusana and from the coast to Chaclacayo. */
    private static final double SOUTH = -12.55;
    private static final double NORTH = -11.55;
    private static final double WEST = -77.25;
    private static final double EAST = -76.60;

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final NominatimRateLimiter rateLimiter;

    public NominatimGeocodingAdapter(NominatimProperties properties, CircuitBreaker circuitBreaker,
                                     NominatimRateLimiter rateLimiter) {
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }

    @Override
    public GeocodingResult geocode(String address) {
        try {
            var result = circuitBreaker.executeCallable(() -> search(address));
            LOGGER.info("nominatim.geocode.completed outcome={}", result.outcome());
            return result;
        } catch (CallNotPermittedException exception) {
            LOGGER.warn("nominatim.geocode.unavailable reason=circuit-open");
            return GeocodingResult.unavailable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("nominatim.geocode.unavailable reason=interrupted");
            return GeocodingResult.unavailable();
        } catch (Exception exception) {
            // Only the class: the message of a client error carries the URL, and with it the address.
            LOGGER.warn("nominatim.geocode.unavailable reason={}", exception.getClass().getSimpleName());
            return GeocodingResult.unavailable();
        }
    }

    private GeocodingResult search(String address) throws InterruptedException {
        rateLimiter.acquire();
        var places = restClient.get()
                .uri(uri -> uri.path(SEARCH_PATH)
                        .queryParam(QUERY_KEY, QUERY_VARIABLE)
                        .queryParam("format", FORMAT)
                        .queryParam("limit", ONE_RESULT)
                        .queryParam("countrycodes", PERU)
                        .build(Map.of(QUERY_KEY, address.strip() + CITY_SUFFIX)))
                .retrieve()
                .body(Place[].class);
        if (places == null || places.length == 0) {
            return GeocodingResult.notFound();
        }
        var point = places[0].toPoint();
        return isInLimaOrCallao(point) ? GeocodingResult.found(point) : GeocodingResult.notFound();
    }

    private static boolean isInLimaOrCallao(GeoPoint point) {
        return point.latitude() >= SOUTH && point.latitude() <= NORTH
                && point.longitude() >= WEST && point.longitude() <= EAST;
    }

    private static JdkClientHttpRequestFactory requestFactory(NominatimProperties properties) {
        var httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    /** Nominatim sends the coordinates as text. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Place(String lat, String lon) {
        GeoPoint toPoint() {
            return new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon));
        }
    }
}
