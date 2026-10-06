package com.geopslabs.geops.reservation.infrastructure.catalog;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.exceptions.CatalogUnavailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class CatalogOfferCatalogAdapter implements OfferCatalogPort {
    public static final String AVAILABILITY_PATH = "/internal/v1/offers/{id}/availability";
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogOfferCatalogAdapter.class);

    private final RestClient restClient;

    public CatalogOfferCatalogAdapter(RestClient.Builder builder, @Value("${geops.catalog.base-url}") String baseUrl) {
        var httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public OfferSnapshot findValidOffer(Long offerId) {
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
