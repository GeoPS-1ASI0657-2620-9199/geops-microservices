package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.RedeemReservationUseCase;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.commands.RedeemReservationCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAlreadyRedeemedException;
import com.geopslabs.geops.reservation.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaEntity;
import com.geopslabs.geops.reservation.infrastructure.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QAS07: ten devices of the business confirm the same code at once against a real PostgreSQL. The conditional
 * update lets exactly one of them redeem it; the other nine get the date of that redemption.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"geops.outbox.relay-enabled=false", "spring.datasource.hikari.maximum-pool-size=12"})
class RedeemReservationConcurrencyTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final int THREADS = 10;
    private static final String CODE = "7KQ2M9XA";
    private static final Long BUSINESS_ID = 84L;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private RedeemReservationUseCase redeemReservation;
    @Autowired
    private ReservationJpaRepository reservations;
    @Autowired
    private OutboxEventJpaRepository outbox;

    @BeforeEach
    void storeOneActiveReservation() {
        outbox.deleteAll();
        reservations.deleteAll();
        var entity = new ReservationJpaEntity();
        entity.setCode(CODE);
        entity.setConsumerId(2001L);
        entity.setOfferId(1052L);
        entity.setBusinessId(BUSINESS_ID);
        entity.setOfferTitle("2x1 en almuerzos ejecutivos");
        entity.setReservedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        entity.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        entity.setStatus(ReservationStatus.ACTIVE);
        reservations.save(entity);
    }

    @Test
    void tenSimultaneousRedemptionsRedeemOnceAndTheOthersGetTheDate()
            throws InterruptedException, ExecutionException {
        var start = new CountDownLatch(1);
        Callable<Object> redeem = () -> {
            start.await();
            try {
                return redeemReservation.redeem(new RedeemReservationCommand(CODE, BUSINESS_ID));
            } catch (ReservationAlreadyRedeemedException rejected) {
                return rejected;
            }
        };
        var executor = Executors.newFixedThreadPool(THREADS);
        var outcomes = new ArrayList<Object>();
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int thread = 0; thread < THREADS; thread++) {
                futures.add(executor.submit(redeem));
            }
            start.countDown();
            for (var future : futures) {
                outcomes.add(future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        var rejected = outcomes.stream()
                .filter(ReservationAlreadyRedeemedException.class::isInstance)
                .map(ReservationAlreadyRedeemedException.class::cast)
                .toList();
        assertThat(outcomes).hasSize(THREADS);
        assertThat(THREADS - rejected.size()).as(outcomes.toString()).isEqualTo(1);
        var stored = reservations.findByCode(CODE).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ReservationStatus.REDEEMED);
        assertThat(rejected).allSatisfy(exception -> assertThat(exception.getRedeemedAt()).isNotNull());
        assertThat(outbox.count()).isEqualTo(1);
    }
}
