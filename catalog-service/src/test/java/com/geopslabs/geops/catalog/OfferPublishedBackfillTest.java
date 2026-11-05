package com.geopslabs.geops.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.infrastructure.messaging.EventContract;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OfferPublishedBackfillTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration";
    private static final String BEFORE_BACKFILL = "4";
    private static final String CAMPAIGN_START_IN_UTC = "2026-10-27T05:00:00Z";
    private static final long ACTIVE_CAMPAIGN = 77L;
    private static final String SEED = """
            INSERT INTO merchant_standings (business_id, business_name, ruc_verified, open_reports, compliance_index,
                                            verified_seal, updated_at)
            VALUES (84, 'Restaurante Don Pepe', true, 0, 100.00, true, now());
            INSERT INTO campaigns (id, business_id, name, description, start_date, end_date, status, zone_type,
                                   zone_center, zone_radius_m, zone_district, estimated_budget)
            VALUES (77, 84, 'Almuerzos de noviembre', 'Menú ejecutivo', DATE '2026-10-27', CURRENT_DATE + 30,
                    'ACTIVE', 'RADIUS', ST_SetSRID(ST_MakePoint(-77.0301, -12.1205), 4326)::geography, 1200, NULL,
                    300.00),
                   (78, 84, 'Ceviches en pausa', 'Ceviche 2x1', DATE '2026-10-27', CURRENT_DATE + 30, 'PAUSED',
                    'DISTRICT', ST_SetSRID(ST_MakePoint(-77.0301, -12.1205), 4326)::geography, NULL, 'Miraflores',
                    0.00);
            INSERT INTO offers (id, campaign_id, business_id, title, conditions, price, valid_to, category,
                                geocoding_status, address, image_url, source, source_name, status, location)
            VALUES (1052, 77, 84, '2x1 en almuerzos ejecutivos', 'Lunes a viernes', 18.50, CURRENT_DATE + 10,
                    'Gastronomía', 'GEOCODED', 'Av. Larco 345', NULL, 'AFFILIATED', NULL, 'PUBLISHED',
                    ST_SetSRID(ST_MakePoint(-77.0297, -12.1211), 4326)::geography),
                   (1053, 77, 84, 'Postre gratis con el menú', 'Con menú', 12.00, CURRENT_DATE + 20,
                    'Gastronomía', 'GEOCODED', 'Av. Larco 345', NULL, 'AFFILIATED', NULL, 'PUBLISHED',
                    ST_SetSRID(ST_MakePoint(-77.0297, -12.1211), 4326)::geography),
                   (1054, 77, 84, 'Vencida sin marcar', 'Antes', 10.00, CURRENT_DATE - 3, 'Gastronomía',
                    'GEOCODED', 'Av. Larco 345', NULL, 'AFFILIATED', NULL, 'PUBLISHED', NULL),
                   (1055, 77, 84, 'Ya vencida', 'Antes', 10.00, CURRENT_DATE + 5, 'Gastronomía', 'GEOCODED',
                    'Av. Larco 345', NULL, 'AFFILIATED', NULL, 'EXPIRED', NULL),
                   (1056, 78, 84, 'Ceviche 2x1', 'Pausada', 25.00, CURRENT_DATE + 10, 'Gastronomía', 'GEOCODED',
                    'Av. Larco 345', NULL, 'AFFILIATED', NULL, 'PUBLISHED', NULL);
            """;
    private static final String BACKFILLED = """
            SELECT id::text AS id, aggregate_id, payload::text AS payload, status
            FROM outbox_events WHERE event_type = 'OfferPublished' ORDER BY seq""";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withInitScript("acceptance/enable-postgis.sql");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static List<Map<String, Object>> events;

    @BeforeAll
    static void migrateWithOffersPublishedBeforeTheRelay() {
        POSTGRES.start();
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations(MIGRATIONS_LOCATION).target(BEFORE_BACKFILL).load()
                .migrate();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(SEED);
        Flyway.configure().dataSource(dataSource).locations(MIGRATIONS_LOCATION).load().migrate();
        events = jdbc.queryForList(BACKFILLED);
    }

    @Test
    void onlyTheValidOffersOfActiveCampaignsAreAnnounced() throws Exception {
        assertThat(events).extracting(row -> MAPPER.readTree((String) row.get("payload"))
                .path("payload").path("offerId").asLong()).containsExactly(1052L, 1053L);
    }

    @Test
    void eachEventHasTheShapeOfTheContractAndItsRowIdAsEventId() throws Exception {
        for (var row : events) {
            var payload = (String) row.get("payload");
            EventContract.assertSameShape(payload, "OfferPublished");
            var envelope = MAPPER.readTree(payload);
            assertThat(envelope.path("eventId").asText()).isEqualTo(row.get("id"));
            assertThat(envelope.path("aggregateId").asLong()).isEqualTo(ACTIVE_CAMPAIGN);
            assertThat(row.get("aggregate_id")).isEqualTo(ACTIVE_CAMPAIGN);
            assertThat(row.get("status")).isEqualTo("PENDING");
        }
    }

    @Test
    void theEventHappenedWhenTheCampaignStartedSoNotificationDoesNotSendLateNotices() throws Exception {
        var envelope = MAPPER.readTree((String) events.get(0).get("payload"));

        assertThat(envelope.path("occurredAt").asText()).isEqualTo(CAMPAIGN_START_IN_UTC);
        assertThat(envelope.path("payload").path("publishedAt").asText()).isEqualTo(CAMPAIGN_START_IN_UTC);
        assertThat(envelope.path("payload").path("reactivated").asBoolean()).isFalse();
        assertThat(envelope.path("payload").path("zone").path("reachMeters").asInt()).isEqualTo(1200);
        assertThat(envelope.path("payload").path("price").decimalValue()).isEqualByComparingTo("18.50");
    }
}
