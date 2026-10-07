package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RedeemedReservationJpaAdapter.class)
class RedeemedReservationJpaAdapterTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final RedeemedReservation REDEMPTION = new RedeemedReservation(1L, 1L, 1L,
            LocalDateTime.parse("2026-10-08T13:05:00"));
    private static final long SINGLE_ROW = 1L;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private RedeemedReservationJpaAdapter adapter;
    @Autowired
    private RedeemedReservationJpaRepository repository;

    @Test
    void savingTheSameRedemptionTwiceKeepsASingleRow() {
        var first = adapter.saveIfAbsent(REDEMPTION);
        var second = adapter.saveIfAbsent(REDEMPTION);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(repository.count()).isEqualTo(SINGLE_ROW);
    }

    @Test
    void anUnreviewedRedemptionIsFoundForItsConsumerAndBusiness() {
        adapter.saveIfAbsent(REDEMPTION);

        assertThat(adapter.findUnreviewed(REDEMPTION.consumerId(), REDEMPTION.businessId())).contains(REDEMPTION);
        assertThat(adapter.existsFor(REDEMPTION.consumerId(), REDEMPTION.businessId())).isTrue();
    }
}
