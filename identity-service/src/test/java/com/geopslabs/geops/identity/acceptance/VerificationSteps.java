package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.identity.domain.events.BusinessVerified;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.RucVerificationJpaRepository;
import com.geopslabs.geops.identity.infrastructure.sunat.SunatRegistryLoadException;
import com.geopslabs.geops.identity.infrastructure.sunat.SunatRegistryLoader;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US38 and the load of the SUNAT registry. "Business 84" in the feature is an alias: the step keeps the id the
 * database gave it, so scenarios do not depend on the sequences.
 */
public class VerificationSteps {
    private static final String VERIFY_PATH = "/api/v1/business-profiles/%d/ruc-verifications";
    private static final String PROFILE_PATH = "/api/v1/business-profiles/%d";
    private static final String SEARCH_PATH = "/search";
    private static final String CITY_SUFFIX = ", Lima, Perú";
    private static final String DEFAULT_ADDRESS = "Av. Arequipa 1000, Lince";
    private static final GeoPoint MAP_POINT = new GeoPoint(-12.1211, -77.0297);
    private static final String SUNAT_PATH = "/padron_reducido_ruc.zip";
    private static final long UNKNOWN_BUSINESS_OFFSET = 1_000_000L;
    private static final String PLACE = """
            [{"place_id": 1, "lat": "%s", "lon": "%s", "display_name": "%s"}]""";

    private final HttpScenario http;
    private final TokenSteps tokens;
    private final UserRepositoryPort users;
    private final BusinessProfileRepositoryPort businessProfiles;
    private final RucVerificationJpaRepository verifications;
    private final OutboxEventJpaRepository outbox;
    private final SunatRegistryLoader registryLoader;
    private final CircuitBreakerRegistry circuitBreakers;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Map<Long, Owner> owners = new HashMap<>();
    private RuntimeException loadFailure;

    public VerificationSteps(HttpScenario http, TokenSteps tokens, UserRepositoryPort users,
                             BusinessProfileRepositoryPort businessProfiles, RucVerificationJpaRepository verifications,
                             OutboxEventJpaRepository outbox, SunatRegistryLoader registryLoader,
                             CircuitBreakerRegistry circuitBreakers, JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.http = http;
        this.tokens = tokens;
        this.users = users;
        this.businessProfiles = businessProfiles;
        this.verifications = verifications;
        this.outbox = outbox;
        this.registryLoader = registryLoader;
        this.circuitBreakers = circuitBreakers;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Before
    public void resetProviders() {
        CucumberSpringConfiguration.PROVIDERS.resetAll();
        circuitBreakers.circuitBreaker("nominatim").reset();
    }

    @Given("the SUNAT registry sample is loaded")
    public void theRegistrySampleIsLoaded() {
        var report = registryLoader.load();
        assertThat(report.wasSkipped()).isFalse();
    }

    @Given("business owner with business {long} registered RUC {string} at {string}")
    public void aBusinessOwnerWithAddress(long alias, String ruc, String address) {
        var owner = users.save(User.register("Dueño " + alias, new Email("dueno" + alias + "@ejemplo.pe"),
                "9%08d".formatted(alias), "hash", Role.BUSINESS_OWNER));
        var profile = BusinessProfile.register("Negocio " + alias, "Restaurante", new Ruc(ruc), address, MAP_POINT,
                null);
        var saved = businessProfiles.save(profile.ownedBy(owner.getId()));
        owners.put(alias, new Owner(owner.getId(), saved.getId()));
    }

    @Given("business owner with business {long} registered RUC {string}")
    public void aBusinessOwner(long alias, String ruc) {
        aBusinessOwnerWithAddress(alias, ruc, DEFAULT_ADDRESS);
    }

    @Given("Nominatim finds {string} at latitude {double} and longitude {double}")
    public void nominatimFinds(String address, double latitude, double longitude) {
        CucumberSpringConfiguration.PROVIDERS.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("q", equalTo(address + CITY_SUFFIX))
                .willReturn(okJson(PLACE.formatted(latitude, longitude, address))));
    }

    @Given("Nominatim finds nothing for {string}")
    public void nominatimFindsNothing(String address) {
        CucumberSpringConfiguration.PROVIDERS.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .withQueryParam("q", equalTo(address + CITY_SUFFIX))
                .willReturn(okJson("[]")));
    }

    @Given("Nominatim answers {int}")
    public void nominatimAnswers(int status) {
        CucumberSpringConfiguration.PROVIDERS.stubFor(get(urlPathEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(status)));
    }

    @Given("business {long} is VERIFIED")
    public void businessIsVerified(long alias) {
        jdbc.update("UPDATE business_profiles SET verification_status = 'VERIFIED' WHERE id = ?", idOf(alias));
    }

    @Given("the SUNAT registry now lists RUC {string} as {string}")
    public void theRegistryNowLists(String ruc, String status) {
        assertThat(jdbc.update("UPDATE sunat_taxpayers SET taxpayer_status = ? WHERE ruc = ?", status, ruc))
                .isEqualTo(1);
    }

    @When("the owner of business {long} requests the verification")
    public void theOwnerRequestsTheVerification(long alias) {
        http.post(VERIFY_PATH.formatted(idOf(alias)), null, ownerToken(alias));
    }

    @When("the owner of business {long} requests the verification with address {string}")
    public void theOwnerRequestsTheVerificationWithAddress(long alias, String address) {
        http.post(VERIFY_PATH.formatted(idOf(alias)), Map.of("address", address), ownerToken(alias));
    }

    @When("the owner of business {long} requests the verification of business {long}")
    public void anotherOwnerRequestsTheVerification(long requester, long target) {
        http.post(VERIFY_PATH.formatted(idOf(target)), null, ownerToken(requester));
    }

    @When("the owner of business {long} reads the profile")
    public void theOwnerReadsTheProfile(long alias) {
        http.get(PROFILE_PATH.formatted(idOf(alias)), ownerToken(alias));
    }

    @Then("business {long} is VERIFIED with the verification date stored")
    public void businessIsVerifiedWithDate(long alias) {
        assertThat(statusOf(alias)).isEqualTo(VerificationStatus.VERIFIED);
        var attempts = verifications.findByBusinessProfileIdOrderByIdAsc(idOf(alias));
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getCheckedAt()).isNotNull();
        assertThat(attempts.get(0).getTaxpayerStatus()).isEqualTo("ACTIVO");
    }

    @Then("business {long} has no verified seal")
    public void businessHasNoSeal(long alias) {
        assertThat(statusOf(alias)).isNotEqualTo(VerificationStatus.VERIFIED);
    }

    @Then("business {long} is not VERIFIED")
    public void businessIsNotVerified(long alias) {
        businessHasNoSeal(alias);
    }

    @Then("business {long} keeps the address {string}")
    public void businessKeepsTheAddress(long alias, String address) {
        assertThat(businessProfiles.findById(idOf(alias)).orElseThrow().getAddress()).isEqualTo(address);
    }

    @Then("one BusinessVerified event with verified {word} is stored in the outbox")
    public void oneEventIsStored(String verified) throws JsonProcessingException {
        var events = events();
        assertThat(events).hasSize(1);
        var envelope = objectMapper.readTree(events.get(0).getPayload());
        assertThat(envelope.path("eventType").asText()).isEqualTo(BusinessVerified.EVENT_TYPE);
        assertThat(envelope.path("payload").path("verified").asBoolean()).isEqualTo(Boolean.parseBoolean(verified));
    }

    @Then("no BusinessVerified event is stored")
    public void noEventIsStored() {
        assertThat(events()).isEmpty();
    }

    @When("the download of the SUNAT registry fails")
    public void theDownloadFails() {
        var providers = CucumberSpringConfiguration.PROVIDERS;
        providers.stubFor(get(SUNAT_PATH).willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> registryLoader.loadFromUrl(URI.create(providers.baseUrl() + SUNAT_PATH)))
                .isInstanceOf(SunatRegistryLoadException.class)
                .satisfies(failure -> loadFailure = (RuntimeException) failure);
    }

    @Then("the load is reported as failed")
    public void theLoadIsReportedAsFailed() {
        assertThat(loadFailure).isNotNull();
    }

    @Then("the registry has {int} taxpayers")
    public void theRegistryHas(int rows) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sunat_taxpayers", Integer.class)).isEqualTo(rows);
    }

    @Then("the registry lists RUC {string} as {string} and {string}")
    public void theRegistryLists(String ruc, String status, String condition) {
        var row = jdbc.queryForMap("SELECT taxpayer_status, taxpayer_condition FROM sunat_taxpayers WHERE ruc = ?",
                ruc);
        assertThat(row).containsEntry("taxpayer_status", status).containsEntry("taxpayer_condition", condition);
    }

    @Then("the registry does not list RUC {string}")
    public void theRegistryDoesNotList(String ruc) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sunat_taxpayers WHERE ruc = ?", Integer.class, ruc))
                .isZero();
    }

    private List<OutboxEventJpaEntity> events() {
        return outbox.findAll().stream()
                .filter(event -> BusinessVerified.EVENT_TYPE.equals(event.getEventType()))
                .toList();
    }

    private VerificationStatus statusOf(long alias) {
        return businessProfiles.findById(idOf(alias)).orElseThrow().getVerificationStatus();
    }

    private Long idOf(long alias) {
        var owner = owners.get(alias);
        return owner == null ? UNKNOWN_BUSINESS_OFFSET + alias : owner.businessId();
    }

    private String ownerToken(long alias) {
        var owner = owners.get(alias);
        var userId = owner == null ? UNKNOWN_BUSINESS_OFFSET + alias : owner.userId();
        return tokens.tokenFor(userId, Role.BUSINESS_OWNER, idOf(alias));
    }

    private record Owner(Long userId, Long businessId) {
    }
}
