package com.geopslabs.geops.reservation.infrastructure.catalog;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.exceptions.CatalogUnavailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Availability of an offer, asked to Catalog (US34). Each call waits at most 2 s to connect and 2 s to read, goes
 * through the circuit breaker "catalog" and takes a place in the bulkhead "catalog". Five failures in ten seconds
 * open the circuit: from then on the reservation answers CATALOG_UNAVAILABLE at once, without holding a thread on
 * Catalog. An offer that does not exist or is no longer valid is an answer of Catalog, not a failure.
 */
public class CatalogOfferCatalogAdapter implements OfferCatalogPort {
    public static final String AVAILABILITY_PATH = "/internal/v1/offers/{id}/availability";
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogOfferCatalogAdapter.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public CatalogOfferCatalogAdapter(RestClient.Builder builder, String baseUrl, CircuitBreaker circuitBreaker,
                                      Bulkhead bulkhead) {
        var httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
    }

    @Override
    public OfferSnapshot findValidOffer(Long offerId) {
        try {
            return circuitBreaker.executeSupplier(() -> bulkhead.executeSupplier(() -> request(offerId)));
        } catch (CallNotPermittedException exception) {
            LOGGER.warn("catalog.failed offerId={} reason=circuit-open", offerId);
            throw new CatalogUnavailableException();
        } catch (BulkheadFullException exception) {
            LOGGER.warn("catalog.failed offerId={} reason=bulkhead-full", offerId);
            throw new CatalogUnavailableException();
        }
    }

    private OfferSnapshot request(Long offerId) {
        try {
            return restClient.get()
                    .uri(AVAILABILITY_PATH, offerId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange((request, response) -> read(offerId, response));
        } catch (RestClientException exception) {
            LOGGER.warn("catalog.failed offerId={} reason={}", offerId, exception.getClass().getSimpleName());
            throw new CatalogUnavailableException();
        }
    }

    private static OfferSnapshot read(Long offerId, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
            throws IOException {
        var status = response.getStatusCode();
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            throw new OfferNotFoundException(offerId);
        }
        var body = status.is2xxSuccessful() ? response.bodyTo(CatalogOfferResponse.class) : null;
        if (body == null || !body.isComplete()) {
            LOGGER.warn("catalog.failed offerId={} status={}", offerId, status.value());
            throw new CatalogUnavailableException();
        }
        if (!body.available()) {
            throw new OfferNotAvailableException(offerId);
        }
        return new OfferSnapshot(body.offerId(), body.businessId(), body.title(), body.validTo());
    }
}
