package com.geopslabs.geops.identity.infrastructure.decolecta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryUnavailableException;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asks Decolecta for one RUC when a business verifies (US38), instead of keeping a copy of the whole SUNAT registry.
 * It answers RUC 10 (natural persons) too, which the reduced registry lists without district. 404 means SUNAT does
 * not know the RUC; any other failure (timeout, 5xx, 401 for a wrong token or the monthly limit, open circuit
 * breaker) is not an answer and is reported as unavailable. An answer is reused for cacheTtl.
 */
public class DecolectaTaxpayerRegistryAdapter implements TaxpayerRegistryPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(DecolectaTaxpayerRegistryAdapter.class);
    private static final String RUC_PATH = "/v1/sunat/ruc";
    private static final String NUMBER_KEY = "numero";
    private static final String NUMBER_VARIABLE = "{numero}";
    private static final String BEARER = "Bearer ";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final DecolectaProperties properties;
    private final Clock clock;
    private final Map<String, CachedAnswer> answers = new ConcurrentHashMap<>();

    public DecolectaTaxpayerRegistryAdapter(DecolectaProperties properties, CircuitBreaker circuitBreaker,
                                            Clock clock) {
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.clock = clock;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER + properties.token().strip())
                .build();
    }

    @Override
    public Optional<Taxpayer> findByRuc(Ruc ruc) {
        var cached = answers.get(ruc.number());
        if (cached != null && cached.isFresh(clock.instant())) {
            LOGGER.info("decolecta.ruc.cache-hit found={}", cached.taxpayer().isPresent());
            return cached.taxpayer();
        }
        var taxpayer = ask(ruc);
        answers.put(ruc.number(), new CachedAnswer(taxpayer, clock.instant().plus(properties.cacheTtl())));
        return taxpayer;
    }

    @Override
    public boolean isLoaded() {
        return properties.hasToken();
    }

    private Optional<Taxpayer> ask(Ruc ruc) {
        try {
            var taxpayer = circuitBreaker.executeSupplier(() -> lookup(ruc));
            LOGGER.info("decolecta.ruc.completed found={}", taxpayer.isPresent());
            return taxpayer;
        } catch (CallNotPermittedException exception) {
            LOGGER.warn("decolecta.ruc.unavailable reason=circuit-open");
            throw new TaxpayerRegistryUnavailableException("circuit-open");
        } catch (TaxpayerRegistryUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            // Only the class: the message of a client error may carry the request headers.
            LOGGER.warn("decolecta.ruc.unavailable reason={}", exception.getClass().getSimpleName());
            throw new TaxpayerRegistryUnavailableException(exception.getClass().getSimpleName());
        }
    }

    private Optional<Taxpayer> lookup(Ruc ruc) {
        try {
            var answer = restClient.get()
                    .uri(uri -> uri.path(RUC_PATH).queryParam(NUMBER_KEY, NUMBER_VARIABLE)
                            .build(Map.of(NUMBER_KEY, ruc.number())))
                    .retrieve()
                    .body(RucAnswer.class);
            return answer == null || answer.number() == null ? Optional.empty() : Optional.of(answer.toTaxpayer());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                    || exception.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private static JdkClientHttpRequestFactory requestFactory(DecolectaProperties properties) {
        var httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    private record CachedAnswer(Optional<Taxpayer> taxpayer, Instant expiresAt) {
        boolean isFresh(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RucAnswer(@JsonProperty("numero_documento") String number,
                     @JsonProperty("razon_social") String businessName,
                     @JsonProperty("estado") String status,
                     @JsonProperty("condicion") String condition) {
        Taxpayer toTaxpayer() {
            return new Taxpayer(new Ruc(number), businessName, status, condition);
        }
    }
}
