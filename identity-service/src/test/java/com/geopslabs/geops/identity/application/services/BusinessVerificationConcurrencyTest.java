package com.geopslabs.geops.identity.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.identity.acceptance.TestKeys;
import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucCommand;
import com.geopslabs.geops.identity.domain.events.BusinessVerified;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.GeocodingResult;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.geopslabs.geops.identity.infrastructure.sunat.SunatRegistryLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two verifications of the same business at once, against a real PostgreSQL. The geocoder holds both until each
 * thread has its answer, so both reach the write transaction together; the row lock makes the second one apply its
 * result on the state the first one saved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"geops.outbox.relay-enabled=false", "geops.sunat.schedule-enabled=false",
                "geops.verification.retry-enabled=false"})
class BusinessVerificationConcurrencyTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String FOUND_ADDRESS = "Av. José Larco 345, Miraflores";
    private static final String MISSING_ADDRESS = "Av. Larcco 999, Miraflores";
    private static final String OLD_ADDRESS = "Av. Larco 345, Miraflores";
    private static final long WAIT_SECONDS = 20;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Autowired
    private BusinessProfileRepositoryPort businessProfiles;
    @Autowired
    private RucVerificationRepositoryPort verifications;
    @Autowired
    private TaxpayerRegistryPort taxpayerRegistry;
    @Autowired
    private OutboxPort outbox;
    @Autowired
    private UserRepositoryPort users;
    @Autowired
    private SunatRegistryLoader registryLoader;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private final AtomicInteger owners = new AtomicInteger();
    private final AtomicInteger lockedReads = new AtomicInteger();

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        TestKeys.register(registry);
    }

    @BeforeEach
    void cleanAndLoadRegistry() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM ruc_verifications");
        jdbc.update("DELETE FROM business_profiles");
        jdbc.update("DELETE FROM users");
        if (!taxpayerRegistry.isLoaded()) {
            registryLoader.load();
        }
    }

    @RepeatedTest(5)
    void anApprovalAndARejectionAtOnceLeaveTheStatusAndTheEventsInAgreement() throws Exception {
        var businessId = registerBusiness();
        var barrier = new CyclicBarrier(2);
        GeocodingPort geocoder = address -> {
            await(barrier);
            return FOUND_ADDRESS.equals(address) ? GeocodingResult.found(new GeoPoint(-12.1211, -77.0297))
                    : GeocodingResult.notFound();
        };
        var service = service(geocoder);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var approval = executor.submit(() -> service.verify(request(businessId, FOUND_ADDRESS)));
            var rejection = executor.submit(() -> service.verify(request(businessId, MISSING_ADDRESS)));
            approval.get(WAIT_SECONDS, TimeUnit.SECONDS);
            rejection.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        var finalStatus = businessProfiles.findById(businessId).orElseThrow().getVerificationStatus();
        var published = publishedVerifiedFlags(businessId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ruc_verifications WHERE business_profile_id = ?",
                Integer.class, businessId)).isEqualTo(2);
        if (published.isEmpty()) {
            assertThat(finalStatus).isNotEqualTo(VerificationStatus.VERIFIED);
        } else {
            assertThat(published.get(published.size() - 1))
                    .as("the last event Catalog receives matches the status Identity kept")
                    .isEqualTo(finalStatus == VerificationStatus.VERIFIED);
        }
        // With this pair the approval always ends verified: either it runs last, or the rejection that runs
        // after it is a corrected address that a verified business does not take.
        assertThat(finalStatus).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(published).containsExactly(true);
    }

    @Test
    void aRetryThatGeocodedTheOldAddressDoesNotUndoTheApprovalOfTheCorrectedOne() throws Exception {
        var businessId = registerBusiness();
        jdbc.update("UPDATE business_profiles SET verification_status = 'PENDING' WHERE id = ?", businessId);
        var retryIsGeocoding = new CountDownLatch(1);
        var ownerIsDone = new CountDownLatch(1);
        GeocodingPort geocoder = address -> {
            if (OLD_ADDRESS.equals(address)) {
                retryIsGeocoding.countDown();
                await(ownerIsDone);
                return GeocodingResult.notFound();
            }
            return GeocodingResult.found(new GeoPoint(-12.1211, -77.0297));
        };
        var service = service(geocoder);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var retry = executor.submit(() -> service.retry(businessId));
            assertThat(retryIsGeocoding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            var owner = service.verify(request(businessId, FOUND_ADDRESS));
            assertThat(owner.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            ownerIsDone.countDown();

            var stale = retry.get(WAIT_SECONDS, TimeUnit.SECONDS);

            assertThat(stale.verification().reason()).isEqualTo(
                    com.geopslabs.geops.identity.domain.models.VerificationReason.ADDRESS_NOT_GEOCODABLE);
            assertThat(stale.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        } finally {
            executor.shutdownNow();
        }
        var profile = businessProfiles.findById(businessId).orElseThrow();
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(profile.getAddress()).isEqualTo(FOUND_ADDRESS);
        assertThat(publishedVerifiedFlags(businessId)).containsExactly(true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ruc_verifications WHERE business_profile_id = ?",
                Integer.class, businessId)).as("the stale attempt is still stored").isEqualTo(2);
    }

    @Test
    void aVerificationWaitsWhileAnotherTransactionHoldsTheProfile() throws Exception {
        var businessId = registerBusiness();
        var service = service(address -> GeocodingResult.found(new GeoPoint(-12.1211, -77.0297)));
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> {
                        businessProfiles.findByIdForUpdate(businessId).orElseThrow();
                        locked.countDown();
                        await(release);
                    }));
            assertThat(locked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            var verification = executor.submit(() -> service.verify(request(businessId, null)));

            assertThatThrownBy(() -> verification.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(lockedReads.get()).as("the re-read waits for the lock").isZero();
            release.countDown();
            holder.get(WAIT_SECONDS, TimeUnit.SECONDS);

            assertThat(verification.get(WAIT_SECONDS, TimeUnit.SECONDS).verificationStatus())
                    .isEqualTo(VerificationStatus.VERIFIED);
            assertThat(lockedReads.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Each attempt is written a while after the profile is read again, inside the write transaction: 100 ms for an
     * approval, 600 ms for a rejection, so without the row lock both threads read the same old status and the
     * rejection commits last, overwriting the approval that Catalog already heard about.
     */
    private BusinessVerificationService service(GeocodingPort geocoder) {
        RucVerificationRepositoryPort slowVerifications = new RucVerificationRepositoryPort() {
            @Override
            public RucVerification save(RucVerification verification) {
                pause(verification.isApproved() ? 100 : 600);
                return verifications.save(verification);
            }

            @Override
            public Optional<RucVerification> findLatestByBusinessProfileId(Long businessProfileId) {
                return verifications.findLatestByBusinessProfileId(businessProfileId);
            }
        };
        BusinessProfileRepositoryPort observedProfiles = new BusinessProfileRepositoryPort() {
            @Override
            public BusinessProfile save(BusinessProfile businessProfile) {
                return businessProfiles.save(businessProfile);
            }

            @Override
            public Optional<BusinessProfile> findById(Long id) {
                return businessProfiles.findById(id);
            }

            @Override
            public Optional<BusinessProfile> findByIdForUpdate(Long id) {
                var profile = businessProfiles.findByIdForUpdate(id);
                lockedReads.incrementAndGet();
                return profile;
            }

            @Override
            public Optional<BusinessProfile> findByUserId(Long userId) {
                return businessProfiles.findByUserId(userId);
            }

            @Override
            public List<Long> findIdsByVerificationStatus(VerificationStatus status) {
                return businessProfiles.findIdsByVerificationStatus(status);
            }

            @Override
            public boolean existsByUserId(Long userId) {
                return businessProfiles.existsByUserId(userId);
            }

            @Override
            public boolean existsByRuc(Ruc ruc) {
                return businessProfiles.existsByRuc(ruc);
            }
        };
        return new BusinessVerificationService(observedProfiles, slowVerifications, taxpayerRegistry, geocoder,
                outbox, new TransactionTemplate(transactionManager), Clock.systemUTC(), UUID::randomUUID);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private Long registerBusiness() {
        var number = owners.incrementAndGet();
        var owner = users.save(User.register("Dueño " + number, new Email("dueno.carrera" + number + "@ejemplo.pe"),
                "96%07d".formatted(number), "hash", Role.BUSINESS_OWNER));
        var profile = BusinessProfile.register("Restaurante Don Pepe", "Restaurante", new Ruc("20123456786"),
                OLD_ADDRESS, new GeoPoint(-12.1211, -77.0297), null);
        return businessProfiles.save(profile.ownedBy(owner.getId())).getId();
    }

    private static VerifyBusinessRucCommand request(Long businessId, String address) {
        return new VerifyBusinessRucCommand(businessId, businessId, address);
    }

    private List<Boolean> publishedVerifiedFlags(Long businessId) {
        return jdbc.queryForList("SELECT payload::text FROM outbox_events WHERE aggregate_id = ? AND event_type = ? "
                        + "ORDER BY seq", String.class, businessId, BusinessVerified.EVENT_TYPE).stream()
                .map(this::verifiedFlagOf)
                .toList();
    }

    private Boolean verifiedFlagOf(String envelope) {
        try {
            return objectMapper.readTree(envelope).path("payload").path("verified").asBoolean();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
