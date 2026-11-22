package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.geopslabs.geops.catalog.application.services.OfferInteractionCounter;
import com.geopslabs.geops.catalog.application.services.OfferQueryService;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.CampaignRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.infrastructure.messaging.EventContract;
import com.geopslabs.geops.catalog.infrastructure.messaging.ReservationMetricsListener;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/** US11 y US45: métricas de la campaña y línea base del comercio, con los pasos en castellano. */
public class CampaignMetricsSteps {
    private static final String CAMPAIGNS = "/api/v1/campaigns";
    private static final String METRICS = CAMPAIGNS + "/%d/metrics?from=%s&to=%s";
    private static final String SUMMARY = CAMPAIGNS + "/metrics";
    private static final String BASELINE = CAMPAIGNS + "/baseline";
    private static final String NEARBY = "/api/v1/offers/nearby?lat=%s&lng=%s&radiusMinutes=10";
    private static final String OFFER_DETAIL = "/api/v1/offers/%d";
    private static final String BASELINE_BODY = "{\"customersPerWeek\": %d, \"averageTicket\": %s}";
    private static final String INSERT_METRICS = """
            INSERT INTO campaign_daily_metrics (campaign_id, metric_date, impressions, clicks, reservations, conversions)
            VALUES (?, ?, ?, ?, ?, ?)""";
    private static final Long OWNER_USER_ID = 9001L;
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final int RADIUS_METERS = 800;
    private static final BigDecimal BUDGET = new BigDecimal("500.00");
    private static final BigDecimal FULL_COMPLIANCE = new BigDecimal("100.00");
    private static final BigDecimal PRICE = new BigDecimal("15.00");
    private static final LocalTime MIDDAY = LocalTime.NOON;
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final int HTTP_OK = 200;
    private static final int HTTP_NO_CONTENT = 204;

    private final HttpScenario http;
    private final ScenarioState state;
    private final MerchantStandingRepositoryPort standings;
    private final CampaignRepositoryPort campaigns;
    private final OfferRepositoryPort offers;
    private final OfferInteractionCounter counter;
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final KafkaAdmin kafkaAdmin;
    private ObjectNode lastRedemption;

    @SuppressWarnings("java:S107")
    public CampaignMetricsSteps(HttpScenario http, ScenarioState state, MerchantStandingRepositoryPort standings,
                                CampaignRepositoryPort campaigns, OfferRepositoryPort offers,
                                OfferInteractionCounter counter, JdbcTemplate jdbc,
                                KafkaTemplate<String, String> kafka, KafkaAdmin kafkaAdmin) {
        this.http = http;
        this.state = state;
        this.standings = standings;
        this.campaigns = campaigns;
        this.offers = offers;
        this.counter = counter;
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.kafkaAdmin = kafkaAdmin;
    }

    @Dado("que el comercio {long} {string} tiene la campaña {string} del {string} al {string} con la oferta {string} en su local")
    public void comercioTieneCampañaConOferta(Long businessId, String businessName, String campaignName, String start,
                                              String end, String title) {
        // Lo que quedó en memoria de otros escenarios se escribe antes, para que no se mezcle con este.
        counter.flush();
        standings.save(new MerchantStanding(businessId, businessName, false, 0, FULL_COMPLIANCE, false,
                Instant.now()));
        var campaign = saveCampaign(businessId, campaignName, start, end);
        var offer = offers.save(new Offer(null, campaign.getId(), businessId, title, "Un cupón por mesa.",
                Money.soles(PRICE), LocalDate.parse(end), "Gastronomía", GeocodingStatus.GEOCODED,
                "Av. Larco 345, Miraflores", null, OfferSource.AFFILIATED, null, OfferStatus.PUBLISHED, STORE));
        state.rememberOffer(title, offer.getId());
    }

    @Dado("que el comercio {long} también tiene la campaña {string} sin actividad")
    public void comercioTambiénTieneCampaña(Long businessId, String campaignName) {
        saveCampaign(businessId, campaignName, "2026-10-05", "2026-10-20");
    }

    @Dado("que un consumidor buscó ofertas junto al local {int} veces")
    public void consumidorBuscó(int times) {
        for (var search = 0; search < times; search++) {
            var result = http.get(NEARBY.formatted(STORE.latitude(), STORE.longitude()), null);
            assertThat(result.status()).as(result.body()).isEqualTo(HTTP_OK);
            assertThat(result.body()).contains("\"offerId\"");
        }
    }

    @Y("abrió el detalle de {string} {int} veces")
    public void abrióElDetalle(String title, int times) {
        for (var open = 0; open < times; open++) {
            var result = http.get(OFFER_DETAIL.formatted(state.offerId(title)), null);
            assertThat(result.status()).as(result.body()).isEqualTo(HTTP_OK);
        }
    }

    @Cuando("se escriben los contadores de impresiones y clics")
    public void seEscribenLosContadores() {
        counter.flush();
    }

    @Dado("que la campaña {string} registró estas métricas:")
    public void campañaRegistróMétricas(String campaignName, DataTable table) {
        var campaignId = state.campaignId(campaignName);
        for (var row : table.asMaps()) {
            jdbc.update(INSERT_METRICS, campaignId, Date.valueOf(LocalDate.parse(row.get("fecha"))),
                    Long.parseLong(row.get("impresiones")), Long.parseLong(row.get("clics")),
                    Long.parseLong(row.get("reservas")), Long.parseLong(row.get("canjes")));
        }
    }

    @Cuando("Reservation publica {int} ReservationCreated y {int} ReservationRedeemed de la oferta {string} el {string}")
    public void reservationPublica(int created, int redeemed, String title, String date) throws Exception {
        var occurredAt = LocalDate.parse(date).atTime(MIDDAY).atZone(OfferQueryService.OFFER_ZONE).toInstant();
        for (var sent = 0; sent < created; sent++) {
            send(event("ReservationCreated", state.offerId(title), occurredAt));
        }
        for (var sent = 0; sent < redeemed; sent++) {
            lastRedemption = event("ReservationRedeemed", state.offerId(title), occurredAt);
            send(lastRedemption);
        }
        awaitBothConsumers();
    }

    @Y("Reservation vuelve a publicar el mismo ReservationRedeemed")
    public void reservationRepite() throws Exception {
        send(lastRedemption);
        awaitBothConsumers();
    }

    @Cuando("el dueño del comercio {long} consulta las métricas de {string} del {string} al {string}")
    public void consultaMétricas(Long businessId, String campaignName, String from, String to) {
        http.get(METRICS.formatted(state.campaignId(campaignName), from, to), ownerToken(businessId));
    }

    @Cuando("el dueño del comercio {long} consulta el resumen de sus campañas")
    public void consultaResumen(Long businessId) {
        http.get(SUMMARY, ownerToken(businessId));
    }

    @Dado("que Catalog todavía no tiene la copia local del comercio {long}")
    public void catalogNoTieneElComercio(Long businessId) {
        assertThat(standings.findByBusinessId(businessId)).isEmpty();
    }

    // Los decimales van como texto: con "# language: es" Cucumber lee 18.50 con la coma como separador decimal.
    @Cuando("el dueño del comercio {long} declara {int} clientes por semana y un ticket promedio de {string}")
    @Dado("que el comercio {long} declaró {int} clientes por semana y un ticket promedio de {string}")
    public void declaraLíneaBase(Long businessId, int customers, String ticket) {
        http.put(BASELINE, ownerToken(businessId), BASELINE_BODY.formatted(customers, new BigDecimal(ticket)));
    }

    @Entonces("la respuesta tiene el estado {int}")
    public void laRespuestaTieneElEstado(int status) {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(status);
    }

    @Y("el código de error es {string}")
    public void elCódigoDeErrorEs(String code) throws JsonProcessingException {
        assertThat(body().path("code").asText()).isEqualTo(code);
    }

    @Entonces("los totales son {long} impresiones, {long} clics, {long} reservas y {long} canjes")
    public void losTotalesSon(long impressions, long clicks, long reservations, long redemptions)
            throws JsonProcessingException {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(HTTP_OK);
        var totals = body().path("totals");
        assertThat(totals.path("impressions").asLong()).isEqualTo(impressions);
        assertThat(totals.path("clicks").asLong()).isEqualTo(clicks);
        assertThat(totals.path("reservations").asLong()).isEqualTo(reservations);
        assertThat(totals.path("redemptions").asLong()).isEqualTo(redemptions);
    }

    @Y("la tasa de clics es {string}")
    public void laTasaDeClicsEs(String rate) throws JsonProcessingException {
        assertThat(body().path("rates").path("clickThroughRate").decimalValue())
                .isEqualByComparingTo(new BigDecimal(rate));
    }

    @Y("la conversión es {string}")
    public void laConversiónEs(String rate) throws JsonProcessingException {
        assertThat(body().path("rates").path("conversionRate").decimalValue())
                .isEqualByComparingTo(new BigDecimal(rate));
    }

    @Y("la reputación del comercio {long} también contó {int} canje")
    public void laReputaciónContó(Long businessId, int redeemed) {
        assertThat(jdbc.queryForObject("SELECT redeemed_reservations FROM merchant_standings WHERE business_id = ?",
                Integer.class, businessId)).isEqualTo(redeemed);
    }

    @Entonces("la serie diaria tiene {int} días y el {string} está en cero")
    public void laSerieDiaria(int days, String emptyDay) throws JsonProcessingException {
        var daily = body().path("daily");
        assertThat(daily.size()).isEqualTo(days);
        var day = StreamSupport.stream(daily.spliterator(), false)
                .filter(entry -> emptyDay.equals(entry.path("date").asText()))
                .findFirst().orElseThrow();
        assertThat(day.path("impressions").asLong() + day.path("clicks").asLong()
                + day.path("reservations").asLong() + day.path("redemptions").asLong()).isZero();
    }

    @Entonces("el resumen tiene {int} campañas")
    public void elResumenTiene(int count) throws JsonProcessingException {
        assertThat(http.last().status()).as(http.last().body()).isEqualTo(HTTP_OK);
        assertThat(body().path("campaigns").size()).isEqualTo(count);
    }

    @Y("{string} tiene {long} impresiones en el resumen")
    public void tieneImpresionesEnElResumen(String campaignName, long impressions) throws JsonProcessingException {
        var row = StreamSupport.stream(body().path("campaigns").spliterator(), false)
                .filter(entry -> campaignName.equals(entry.path("name").asText()))
                .findFirst().orElseThrow();
        assertThat(row.path("totals").path("impressions").asLong()).isEqualTo(impressions);
    }

    @Y("la línea base del comercio {long} tiene {int} clientes por semana, ticket {string} y fecha {string}")
    public void laLíneaBaseTiene(Long businessId, int customers, String ticket, String declaredAt)
            throws JsonProcessingException {
        var stored = http.get(BASELINE, ownerToken(businessId));
        assertThat(stored.status()).isEqualTo(HTTP_OK);
        var baseline = http.json(stored);
        assertThat(baseline.path("customersPerWeek").asInt()).isEqualTo(customers);
        assertThat(baseline.path("averageTicket").decimalValue()).isEqualByComparingTo(new BigDecimal(ticket));
        assertThat(Instant.parse(baseline.path("declaredAt").asText())).isEqualTo(Instant.parse(declaredAt));
    }

    @Entonces("la comparación espera {string} clientes y la campaña suma {long} con un cambio de {string} %")
    public void laComparación(String expected, long customers, String change) throws JsonProcessingException {
        var baseline = body().path("baseline");
        assertThat(baseline.path("expectedCustomers").decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
        assertThat(baseline.path("campaignCustomers").asLong()).isEqualTo(customers);
        assertThat(baseline.path("customersChangePercent").decimalValue()).isEqualByComparingTo(new BigDecimal(change));
    }

    @Y("las ventas estimadas por la campaña son {string}")
    public void lasVentasEstimadas(String sales) throws JsonProcessingException {
        assertThat(body().path("baseline").path("estimatedExtraSales").decimalValue())
                .isEqualByComparingTo(new BigDecimal(sales));
    }

    @Y("la respuesta no trae línea base")
    public void sinLíneaBase() throws JsonProcessingException {
        assertThat(body().path("baseline").isNull()).isTrue();
        assertThat(body().path("totals").isObject()).isTrue();
    }

    @Y("la consulta de la línea base del comercio {long} responde sin contenido")
    public void líneaBaseSinContenido(Long businessId) {
        assertThat(http.get(BASELINE, ownerToken(businessId)).status()).isEqualTo(HTTP_NO_CONTENT);
    }

    private Campaign saveCampaign(Long businessId, String campaignName, String start, String end) {
        var campaign = campaigns.save(new Campaign(null, businessId, campaignName, campaignName,
                new DateRange(LocalDate.parse(start), LocalDate.parse(end)),
                new CampaignZone(ZoneType.RADIUS, STORE, RADIUS_METERS, null), CampaignStatus.ACTIVE,
                Money.soles(BUDGET)));
        state.rememberCampaign(campaignName, campaign.getId());
        return campaign;
    }

    private JsonNode body() throws JsonProcessingException {
        return http.json(http.last());
    }

    private static String ownerToken(Long businessId) {
        return TestIdentity.businessOwnerToken(OWNER_USER_ID, businessId);
    }

    private static ObjectNode event(String eventType, Long offerId, Instant occurredAt) {
        var envelope = (ObjectNode) EventContract.example(eventType);
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("occurredAt", occurredAt.toString());
        ((ObjectNode) envelope.path("payload")).put("offerId", offerId);
        return envelope;
    }

    private void send(ObjectNode envelope) throws Exception {
        kafka.send(ReservationEventSteps.TOPIC, envelope.path("aggregateId").asText(), envelope.toString())
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void awaitBothConsumers() throws Exception {
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, ReservationEventSteps.TOPIC);
        ConsumerGroupProgress.awaitConsumed(kafkaAdmin, ReservationEventSteps.TOPIC, ReservationMetricsListener.GROUP);
    }
}
