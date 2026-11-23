package com.geopslabs.geops.reservation.acceptance;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Catalog on a local port for the real CatalogOfferCatalogAdapter (US34): it answers the availability endpoint
 * with the status and body it was given and counts the requests that reached it.
 */
final class StubCatalogServer {
    private static final String LOOPBACK = "127.0.0.1";
    private static final int ANY_FREE_PORT = 0;
    private static final int NO_BACKLOG = 0;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;
    private static final String ERROR_BODY = "{\"code\": \"INTERNAL_ERROR\"}";

    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static final AtomicInteger STATUS = new AtomicInteger(HTTP_SERVICE_UNAVAILABLE);
    private static final AtomicReference<String> BODY = new AtomicReference<>(ERROR_BODY);
    private static final HttpServer SERVER = start();

    private StubCatalogServer() {
    }

    static String baseUrl() {
        return "http://" + LOOPBACK + ":" + SERVER.getAddress().getPort();
    }

    static void answer(int status, String body) {
        STATUS.set(status);
        BODY.set(body);
        REQUESTS.set(0);
    }

    static void fail() {
        answer(HTTP_SERVICE_UNAVAILABLE, ERROR_BODY);
    }

    static int requests() {
        return REQUESTS.get();
    }

    private static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress(LOOPBACK, ANY_FREE_PORT), NO_BACKLOG);
            server.createContext("/", exchange -> {
                REQUESTS.incrementAndGet();
                var bytes = BODY.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(STATUS.get(), bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
