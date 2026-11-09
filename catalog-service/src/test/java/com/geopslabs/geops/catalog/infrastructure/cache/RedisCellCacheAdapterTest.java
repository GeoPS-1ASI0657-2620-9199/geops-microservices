package com.geopslabs.geops.catalog.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCellCacheAdapterTest {
    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);
    private static final String CELL = "6mc5k8";
    private static final String OTHER_CELL = "6mc5k9";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration PAUSE = Duration.ofSeconds(5);
    private static final GeoPoint POINT = new GeoPoint(-12.1211, -77.0297);

    private static LettuceConnectionFactory connections;
    private static StringRedisTemplate redis;

    private final AtomicLong nanos = new AtomicLong();
    private SimpleMeterRegistry meters;
    private RedisCellCacheAdapter cache;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        var client = LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(200)).build();
        connections = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT)), client);
        connections.afterPropertiesSet();
        connections.start();
        redis = new StringRedisTemplate(connections);
    }

    @AfterAll
    static void stopRedis() {
        connections.destroy();
        REDIS.stop();
    }

    @BeforeEach
    void setUp() {
        redis.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        meters = new SimpleMeterRegistry();
        var json = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new RedisCellCacheAdapter(redis, json, meters, TTL, PAUSE, nanos::get);
    }

    @Test
    void storesACellForTenMinutesAndReadsItBack() {
        cache.put(CELL, null, List.of(offer(1L)));

        var lookup = cache.lookup(Set.of(CELL, OTHER_CELL));

        assertThat(lookup.hits()).containsOnlyKeys(CELL);
        assertThat(lookup.hits().get(CELL)).containsExactly(offer(1L));
        assertThat(redis.getExpire("catalog:cell:v1:" + CELL)).isBetween(595L, 600L);
        assertThat(meters.counter("catalog.cell.cache.hits").count()).isEqualTo(1);
        assertThat(meters.counter("catalog.cell.cache.misses").count()).isEqualTo(1);
    }

    @Test
    void evictingDeletesTheCell() {
        cache.put(CELL, null, List.of(offer(1L)));

        cache.evict(Set.of(CELL));

        assertThat(cache.lookup(Set.of(CELL)).hits()).isEmpty();
        assertThat(redis.hasKey("catalog:cell:v1:" + CELL)).isFalse();
    }

    @Test
    void aSearchThatReadTheDatabaseBeforeAnEvictionDoesNotWriteTheOldCell() {
        var before = cache.lookup(Set.of(CELL));

        cache.evict(Set.of(CELL));
        cache.put(CELL, before.versions().get(CELL), List.of(offer(1L)));

        assertThat(cache.lookup(Set.of(CELL)).hits()).isEmpty();
    }

    @Test
    void aSearchAfterTheEvictionWritesTheCell() {
        cache.evict(Set.of(CELL));
        var after = cache.lookup(Set.of(CELL));

        cache.put(CELL, after.versions().get(CELL), List.of(offer(2L)));

        assertThat(cache.lookup(Set.of(CELL)).hits().get(CELL)).containsExactly(offer(2L));
    }

    @Test
    void aRedisThatDoesNotAnswerIsAMissAndIsSkippedForAWhile() {
        cache.put(CELL, null, List.of(offer(1L)));
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            assertThat(cache.lookup(Set.of(CELL)).hits()).isEmpty();
            assertThat(meters.counter("catalog.cell.cache.failed").count()).isEqualTo(1);

            var start = System.nanoTime();
            assertThat(cache.lookup(Set.of(CELL)).hits()).isEmpty();
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(100));
            assertThat(meters.counter("catalog.cell.cache.failed").count()).isEqualTo(1);
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
        nanos.addAndGet(PAUSE.toNanos());

        assertThat(cache.lookup(Set.of(CELL)).hits()).containsOnlyKeys(CELL);
    }

    private static NearbyOfferCandidate offer(Long id) {
        return new NearbyOfferCandidate(id, "Menú ejecutivo", new BigDecimal("15.00"), LocalDate.of(2026, 10, 15),
                "Gastronomía", "Av. Larco 345, Miraflores", null, POINT, 84L, "Restaurante Don Pepe", true, 0,
                new CampaignZone(ZoneType.RADIUS, POINT, 800, null));
    }
}
