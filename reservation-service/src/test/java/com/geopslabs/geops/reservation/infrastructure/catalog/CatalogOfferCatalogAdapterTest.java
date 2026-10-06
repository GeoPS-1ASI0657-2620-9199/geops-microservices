package com.geopslabs.geops.reservation.infrastructure.catalog;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.exceptions.CatalogUnavailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogOfferCatalogAdapterTest {
    private static final String LOOPBACK = "127.0.0.1";
    private static final int ANY_FREE_PORT = 0;
    private static final int NO_BACKLOG = 0;
    private static final int IMMEDIATELY = 0;
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_SERVER_ERROR = 500;
    private static final String BASE_URL = "http://" + LOOPBACK + ":";
    private static final Long OFFER_ID = 1052L;
    private static final String AVAILABLE_OFFER = """
            {"offerId": 1052, "businessId": 301, "title": "Menú ejecutivo a mitad de precio",
             "validTo": "2026-10-14", "available": true}""";
    private static final String UNAVAILABLE_OFFER = """
            {"offerId": 1052, "businessId": 301, "title": "Menú ejecutivo a mitad de precio",
             "validTo": "2026-10-14", "available": false}""";
    private static final String INCOMPLETE_OFFER = """
            {"offerId": 1052, "title": "Menú ejecutivo a mitad de precio"}""";
    private static final String NOT_FOUND_BODY = """
            {"code": "OFFER_NOT_FOUND", "message": "Offer 1052 was not found"}""";

    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private final AtomicReference<Integer> status = new AtomicReference<>(HTTP_OK);
    private final AtomicReference<String> body = new AtomicReference<>(AVAILABLE_OFFER);
    private HttpServer catalog;

    @BeforeEach
    void startCatalogStub() throws IOException {
        catalog = HttpServer.create(new InetSocketAddress(LOOPBACK, ANY_FREE_PORT), NO_BACKLOG);
        catalog.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            var bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        catalog.start();
    }

    @AfterEach
    void stopCatalogStub() {
        catalog.stop(IMMEDIATELY);
    }

    @Test
    void readsTitleBusinessAndValidityFromTheAvailabilityEndpoint() {
        var offer = adapter().findValidOffer(OFFER_ID);

        assertThat(requestedPath.get()).isEqualTo("/internal/v1/offers/1052/availability");
        assertThat(offer).isEqualTo(new OfferSnapshot(OFFER_ID, 301L, "Menú ejecutivo a mitad de precio",
                LocalDate.parse("2026-10-14")));
    }

    @Test
    void offerThatCatalogMarksAsUnavailableIsNotAvailable() {
        body.set(UNAVAILABLE_OFFER);

        assertThatThrownBy(() -> adapter().findValidOffer(OFFER_ID)).isInstanceOf(OfferNotAvailableException.class);
    }

    @Test
    void notFoundFromCatalogIsAnUnknownOffer() {
        status.set(HTTP_NOT_FOUND);
        body.set(NOT_FOUND_BODY);

        assertThatThrownBy(() -> adapter().findValidOffer(OFFER_ID)).isInstanceOf(OfferNotFoundException.class);
    }

    @Test
    void serverErrorFromCatalogMeansCatalogIsUnavailable() {
        status.set(HTTP_SERVER_ERROR);
        body.set(NOT_FOUND_BODY);

        assertThatThrownBy(() -> adapter().findValidOffer(OFFER_ID)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void incompleteAnswerMeansCatalogIsUnavailable() {
        body.set(INCOMPLETE_OFFER);

        assertThatThrownBy(() -> adapter().findValidOffer(OFFER_ID)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void catalogDownMeansCatalogIsUnavailable() {
        var adapter = adapter();
        catalog.stop(IMMEDIATELY);

        assertThatThrownBy(() -> adapter.findValidOffer(OFFER_ID)).isInstanceOf(CatalogUnavailableException.class);
    }

    private CatalogOfferCatalogAdapter adapter() {
        return new CatalogOfferCatalogAdapter(RestClient.builder(), BASE_URL + catalog.getAddress().getPort());
    }
}
