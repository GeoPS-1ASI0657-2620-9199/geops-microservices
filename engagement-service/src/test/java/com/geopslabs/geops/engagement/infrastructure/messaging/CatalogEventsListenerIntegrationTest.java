package com.geopslabs.geops.engagement.infrastructure.messaging;

import com.geopslabs.geops.engagement.domain.ports.*;
import com.geopslabs.geops.engagement.application.usecases.ProjectCatalogEventsUseCase;
import com.geopslabs.geops.engagement.domain.models.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "geops.outbox.relay-enabled=false")
@Import(KafkaTestcontainer.class)
class CatalogEventsListenerIntegrationTest {
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));
    static { POSTGRES.start(); }
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired OfferSnapshotRepositoryPort offers;
    @Autowired RedeemedReservationRepositoryPort redemptions;
    @Autowired ProjectCatalogEventsUseCase projector;

    @Test
    void kafkaPublishesTheLocalCopyAndRedemptionsAndLateEventsDoNotUndoPause() throws Exception {
        kafka.send("catalog.events", "77", fixture("OfferPublished")).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(offers.findById(1052L)).isPresent());
        kafka.send("reservation.events", "5012", fixture("ReservationRedeemed")).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(redemptions.findUnreviewed(2001L, 84L)).isNotEmpty());
        projector.campaignPaused(List.of(1052L), Instant.parse("2026-10-28T10:00:00Z"));
        kafka.send("catalog.events", "77", fixture("OfferPublished")).get();
        await().atMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(1)).untilAsserted(() -> assertThat(offers.findById(1052L).orElseThrow().status()).isEqualTo(OfferSnapshot.PAUSED));
        projector.offerExpired(1052L, Instant.parse("2026-11-16T05:00:00Z"));
        assertThat(offers.findById(1052L).orElseThrow().status()).isEqualTo(OfferSnapshot.EXPIRED);
    }

    private String fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/events/" + name + ".v1.json")) { return new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
    }
}
