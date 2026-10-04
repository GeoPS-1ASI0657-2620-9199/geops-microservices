package com.geopslabs.geops.reservation.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationSteps {
    static final String RESERVATIONS = "/api/v1/reservations";
    private static final String OFFER_BODY = "{\"offerId\": %d}";
    private static final String EMPTY_BODY = "{}";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long BUSINESS_ID = 301L;
    private static final Long CONSUMER_ID = 2001L;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_OK = 200;
    private static final int SINGLE_RESERVATION = 1;

    private final HttpScenario http;
    private final FakeOfferCatalog catalog;
    private final ReservationJpaRepository reservations;
    private final ScenarioState state;

    public ReservationSteps(HttpScenario http, FakeOfferCatalog catalog, ReservationJpaRepository reservations,
                            ScenarioState state) {
        this.http = http;
        this.catalog = catalog;
        this.reservations = reservations;
        this.state = state;
    }

    @Before
    public void startWithEmptyReservations() {
        reservations.deleteAll();
        catalog.reset();
    }

    @Dado("que Catalog tiene la oferta {long} del comercio {long} titulada {string} vigente hasta el {string}")
    public void catalogHasOffer(Long offerId, Long businessId, String title, String validTo) {
        catalog.publish(new OfferSnapshot(offerId, businessId, title, LocalDate.parse(validTo)));
    }

    @Dado("que Catalog marca la oferta {long} como no disponible")
    public void catalogMarksOfferUnavailable(Long offerId) {
        catalog.markUnavailable(offerId);
    }

    @Dado("que Catalog no responde")
    public void catalogIsDown() {
        catalog.goDown();
    }

    @Dado("que el consumidor {long} ya reservó la oferta {long}")
    public void consumerAlreadyReserved(Long consumerId, Long offerId) throws JsonProcessingException {
        var result = reserve(TestIdentity.consumerToken(consumerId), offerId);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_CREATED);
        var body = http.json(result);
        state.rememberReservation(body.path("reservationId").asLong(), body.path("code").asText());
    }

    @Dado("que esa reserva ya fue canjeada")
    public void thatReservationWasRedeemed() {
        var entity = reservations.findById(state.reservationId()).orElseThrow();
        entity.setStatus(ReservationStatus.REDEEMED);
        entity.setRedeemedAt(entity.getReservedAt());
        reservations.save(entity);
    }

    @Cuando("el consumidor {long} reserva la oferta {long}")
    public void consumerReserves(Long consumerId, Long offerId) {
        reserve(TestIdentity.consumerToken(consumerId), offerId);
    }

    @Cuando("el consumidor {long} envía una reserva sin offerId")
    public void consumerReservesWithoutOffer(Long consumerId) {
        http.post(RESERVATIONS, TestIdentity.consumerToken(consumerId), EMPTY_BODY);
    }

    @Cuando("se reserva la oferta {long} {}")
    public void reserveWithToken(Long offerId, String tokenKind) {
        reserve(tokenOfKind(tokenKind), offerId);
    }

    @Cuando("el consumidor {long} envía {int} reservas simultáneas de la oferta {long}")
    public void consumerReservesConcurrently(Long consumerId, int requests, Long offerId)
            throws InterruptedException, ExecutionException {
        var request = http.postLater(RESERVATIONS, TestIdentity.consumerToken(consumerId),
                OFFER_BODY.formatted(offerId));
        var start = new CountDownLatch(SINGLE_RESERVATION);
        Callable<HttpScenario.HttpResult> afterStart = () -> {
            start.await();
            return request.call();
        };
        var executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<HttpScenario.HttpResult>> futures = new ArrayList<>();
            for (int index = 0; index < requests; index++) {
                futures.add(executor.submit(afterStart));
            }
            start.countDown();
            for (var future : futures) {
                state.concurrentResults().add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Entonces("la respuesta solo trae los campos reservationId, code y expiresAt")
    public void responseHasOnlyCreationFields() throws JsonProcessingException {
        var fields = new HashSet<String>();
        http.json(http.last()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("reservationId", "code", "expiresAt");
    }

    @Y("el campo {string} tiene {int} caracteres")
    public void fieldHasLength(String field, int length) throws JsonProcessingException {
        assertThat(http.json(http.last()).path(field).asText()).hasSize(length);
    }

    @Y("la cabecera Location apunta a la reserva creada")
    public void locationPointsToReservation() throws JsonProcessingException {
        var id = http.json(http.last()).path("reservationId").asLong();
        assertThat(http.last().location()).endsWith(RESERVATIONS + "/" + id);
    }

    @Y("la reserva guardada está en {word} con el título {string} y el comercio {long}")
    public void storedReservationHas(String status, String title, Long businessId) throws JsonProcessingException {
        var id = http.json(http.last()).path("reservationId").asLong();
        var entity = reservations.findById(id).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.valueOf(status));
        assertThat(entity.getOfferTitle()).isEqualTo(title);
        assertThat(entity.getBusinessId()).isEqualTo(businessId);
    }

    @Y("la respuesta es la misma reserva de antes")
    public void responseIsTheSameReservation() throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isEqualTo(state.reservationId());
        assertThat(body.path("code").asText()).isEqualTo(state.reservationCode());
    }

    @Y("la respuesta es una reserva nueva")
    public void responseIsANewReservation() throws JsonProcessingException {
        var body = http.json(http.last());
        assertThat(body.path("reservationId").asLong()).isNotEqualTo(state.reservationId());
        assertThat(body.path("code").asText()).isNotEqualTo(state.reservationCode());
    }

    @Entonces("una respuesta tiene código 201 y las demás 200 con la misma reserva")
    public void oneCreatedOthersReturnedSame() throws JsonProcessingException {
        var results = state.concurrentResults();
        var created = results.stream().filter(result -> result.status() == HTTP_CREATED).count();
        var returned = results.stream().filter(result -> result.status() == HTTP_OK).count();
        assertThat(created).as(results.toString()).isEqualTo(SINGLE_RESERVATION);
        assertThat(created + returned).as(results.toString()).isEqualTo(results.size());
        var ids = new HashSet<Long>();
        for (var result : results) {
            ids.add(http.json(result).path("reservationId").asLong());
        }
        assertThat(ids).hasSize(SINGLE_RESERVATION);
    }

    @Y("el consumidor {long} tiene {int} reserva(s) en {word} de la oferta {long}")
    public void consumerHasReservations(Long consumerId, int expected, String status, Long offerId) {
        var count = reservations.findAll().stream()
                .filter(entity -> entity.getConsumerId().equals(consumerId))
                .filter(entity -> entity.getOfferId().equals(offerId))
                .filter(entity -> entity.getStatus() == ReservationStatus.valueOf(status))
                .count();
        assertThat(count).isEqualTo(expected);
    }

    @Y("no se guardó ninguna reserva")
    public void noReservationStored() {
        assertThat(reservations.count()).isZero();
    }

    private HttpScenario.HttpResult reserve(String token, Long offerId) {
        return http.post(RESERVATIONS, token, OFFER_BODY.formatted(offerId));
    }

    private static String tokenOfKind(String tokenKind) {
        return switch (tokenKind) {
            case "sin token" -> null;
            case "con token de dueño de negocio" -> TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, BUSINESS_ID);
            case "con token sin rol" -> TestIdentity.tokenWithoutRoles(CONSUMER_ID);
            case "con token de otro emisor" -> TestIdentity.tokenFromAnotherIssuer(CONSUMER_ID);
            case "con token para otra audiencia" -> TestIdentity.tokenForAnotherAudience(CONSUMER_ID);
            case "con token firmado con otra clave" -> TestIdentity.tokenSignedWithAnotherKey(CONSUMER_ID);
            default -> throw new IllegalArgumentException(tokenKind);
        };
    }
}
