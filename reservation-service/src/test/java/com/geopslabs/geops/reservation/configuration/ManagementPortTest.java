package com.geopslabs.geops.reservation.configuration;

import com.geopslabs.geops.reservation.acceptance.CucumberSpringConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US34: health, metrics and the state of the circuit breaker toward Catalog answer only on the management port;
 * the service port, the one the gateway routes, does not expose them.
 */
class ManagementPortTest extends CucumberSpringConfiguration {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int servicePort;
    @LocalManagementPort
    private int managementPort;

    @Test
    void theManagementPortIsNotTheServicePort() {
        assertThat(managementPort).isNotEqualTo(servicePort);
    }

    @Test
    void healthMetricsAndCircuitBreakersAnswerOnTheManagementPort() throws Exception {
        assertThat(status(managementPort, "/actuator/health")).isEqualTo(200);
        assertThat(status(managementPort, "/actuator/circuitbreakers")).isEqualTo(200);
        assertThat(status(managementPort, "/actuator/metrics/resilience4j.circuitbreaker.state")).isEqualTo(200);
        assertThat(status(managementPort, "/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls"))
                .isEqualTo(200);
    }

    @Test
    void theServicePortDoesNotExposeMetrics() throws Exception {
        assertThat(status(servicePort, "/actuator/metrics")).isIn(401, 403, 404);
        assertThat(status(servicePort, "/actuator/circuitbreakers")).isIn(401, 403, 404);
    }

    private static int status(int port, String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
