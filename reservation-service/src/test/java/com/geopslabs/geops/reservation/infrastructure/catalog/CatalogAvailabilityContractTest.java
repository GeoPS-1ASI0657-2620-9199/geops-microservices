package com.geopslabs.geops.reservation.infrastructure.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Consumer side of the contract Reservation -> Catalog (US15). Catalog is replaced by a stub that answers exactly
 * the interactions of contracts/catalog-offer-availability.v1.json, the same file catalog-service verifies as
 * provider, so a change on either side that breaks the other fails one of the two builds. Each interaction checks
 * the request Reservation sends and what it makes of the answer.
 */
class CatalogAvailabilityContractTest {
    private static final String CONTRACT = "/contracts/catalog-offer-availability.v1.json";
    private static final String LOOPBACK = "127.0.0.1";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern OFFER_PATH = Pattern.compile("/internal/v1/offers/(\\d+)/availability");

    private final Map<String, JsonNode> responsesByPath = new ConcurrentHashMap<>();
    private final List<String> requests = new ArrayList<>();
    private HttpServer catalog;

    @BeforeEach
    void startCatalogStub() throws IOException {
        interactions().forEach(interaction -> responsesByPath.put(
                interaction.path("request").path("path").asText(), interaction.path("response")));
        catalog = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        catalog.createContext("/", exchange -> {
            var path = exchange.getRequestURI().getPath();
            synchronized (requests) {
                requests.add(exchange.getRequestMethod() + " " + path + " accept="
                        + exchange.getRequestHeaders().getFirst("Accept"));
            }
            var response = responsesByPath.get(path);
            var status = response == null ? 500 : response.path("status").asInt();
            var bytes = response == null ? new byte[0]
                    : MAPPER.writeValueAsString(response.path("body")).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        catalog.start();
    }

    @AfterEach
    void stopCatalogStub() {
        catalog.stop(0);
    }

    @TestFactory
    Stream<DynamicTest> reservationUnderstandsEveryAnswerOfCatalog() {
        return interactions().map(interaction -> DynamicTest.dynamicTest(
                interaction.path("description").asText(), () -> verify(interaction)));
    }

    private void verify(JsonNode interaction) {
        var request = interaction.path("request");
        var response = interaction.path("response");
        var body = response.path("body");
        var path = OFFER_PATH.matcher(request.path("path").asText());
        assertThat(path.matches()).as("path of %s", interaction.path("description").asText()).isTrue();
        var offerId = Long.valueOf(path.group(1));

        if (response.path("status").asInt() == 404) {
            assertThatThrownBy(() -> adapter().findValidOffer(offerId)).isInstanceOf(OfferNotFoundException.class);
        } else if (!body.path("available").asBoolean()) {
            assertThatThrownBy(() -> adapter().findValidOffer(offerId))
                    .isInstanceOf(OfferNotAvailableException.class);
        } else {
            assertThat(adapter().findValidOffer(offerId)).isEqualTo(new OfferSnapshot(offerId,
                    body.path("businessId").asLong(), body.path("title").asText(),
                    LocalDate.parse(body.path("validTo").asText())));
        }
        synchronized (requests) {
            assertThat(requests).last().isEqualTo(request.path("method").asText() + " " + request.path("path").asText()
                    + " accept=application/json");
        }
    }

    private CatalogOfferCatalogAdapter adapter() {
        return new CatalogOfferCatalogAdapter(RestClient.builder(),
                "http://" + LOOPBACK + ":" + catalog.getAddress().getPort());
    }

    private static Stream<JsonNode> interactions() {
        try (var in = CatalogAvailabilityContractTest.class.getResourceAsStream(CONTRACT)) {
            assertThat(in).as("src/test/resources%s", CONTRACT).isNotNull();
            var contract = MAPPER.readTree(in);
            assertThat(contract.path("consumer").asText()).isEqualTo("reservation-service");
            return StreamSupport.stream(contract.path("interactions").spliterator(), false).toList().stream();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
