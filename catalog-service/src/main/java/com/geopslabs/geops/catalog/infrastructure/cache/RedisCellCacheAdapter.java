package com.geopslabs.geops.catalog.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Módulo de Caché por Celda on Redis (Informe 4.3.2.4). Each cell is the JSON of its offers under
 * catalog:cell:v1:{geohash} with a TTL of 10 minutes, next to a version counter under catalog:cell:v1:{geohash}:ver.
 * Evicting a cell deletes it and increments its version; a search writes a cell back only if the version is still
 * the one it read before going to the database, so a publication can never be hidden by a slower search.
 *
 * <p>Any Redis error is logged as catalog.cell.cache.failed and answered as "no cells": the search goes to the
 * database. After a failure, lookups and writes skip Redis for a few seconds so a dead Redis does not add its
 * timeout to every search; evictions are always attempted.</p>
 */
public class RedisCellCacheAdapter implements CellCachePort {
    static final String KEY_PREFIX = "catalog:cell:v1:";
    static final String VERSION_SUFFIX = ":ver";
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCellCacheAdapter.class);
    private static final TypeReference<List<NearbyOfferCandidate>> CELL_TYPE = new TypeReference<>() {
    };
    private static final String NO_VERSION = "";
    private static final long VERSION_TTL_SECONDS = Duration.ofDays(1).toSeconds();
    /** Writes the cell only if its version did not change since the lookup (ARGV[1], empty when it had none). */
    private static final RedisScript<Long> PUT_IF_SAME_VERSION = RedisScript.of("""
            local current = redis.call('GET', KEYS[2])
            if (current == false and ARGV[1] == '') or current == ARGV[1] then
              redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration cellTtl;
    private final Duration pauseAfterFailure;
    private final LongSupplier nanoClock;
    private final Counter hits;
    private final Counter misses;
    private final Counter failures;
    private volatile long skipUntilNanos;
    private volatile boolean skipping;

    public RedisCellCacheAdapter(StringRedisTemplate redis, ObjectMapper objectMapper, MeterRegistry meters,
                                 Duration cellTtl, Duration pauseAfterFailure, LongSupplier nanoClock) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.cellTtl = cellTtl;
        this.pauseAfterFailure = pauseAfterFailure;
        this.nanoClock = nanoClock;
        this.hits = Counter.builder("catalog.cell.cache.hits").description("Cells served from Redis")
                .register(meters);
        this.misses = Counter.builder("catalog.cell.cache.misses").description("Cells read from PostgreSQL")
                .register(meters);
        this.failures = Counter.builder("catalog.cell.cache.failed").description("Redis commands that failed")
                .register(meters);
    }

    @Override
    public Lookup lookup(Set<String> geohashes) {
        if (geohashes.isEmpty()) {
            return Lookup.empty();
        }
        if (isSkipping()) {
            misses.increment(geohashes.size());
            return Lookup.empty();
        }
        var cells = new ArrayList<>(geohashes);
        var keys = new ArrayList<String>(cells.size() * 2);
        cells.forEach(cell -> keys.add(cellKey(cell)));
        cells.forEach(cell -> keys.add(versionKey(cell)));
        try {
            var values = redis.opsForValue().multiGet(keys);
            if (values == null) {
                throw new IllegalStateException("MGET answered nothing");
            }
            var found = new HashMap<String, List<NearbyOfferCandidate>>();
            var versions = new HashMap<String, String>();
            for (var index = 0; index < cells.size(); index++) {
                var json = values.get(index);
                versions.put(cells.get(index), values.get(cells.size() + index));
                if (json != null) {
                    found.put(cells.get(index), objectMapper.readValue(json, CELL_TYPE));
                }
            }
            hits.increment(found.size());
            misses.increment((double) cells.size() - found.size());
            return new Lookup(found, versions);
        } catch (RuntimeException | JsonProcessingException exception) {
            failed("lookup", exception);
            misses.increment(geohashes.size());
            return Lookup.empty();
        }
    }

    @Override
    public void put(String geohash, String version, List<NearbyOfferCandidate> offers) {
        if (isSkipping()) {
            return;
        }
        try {
            redis.execute(PUT_IF_SAME_VERSION, List.of(cellKey(geohash), versionKey(geohash)),
                    version == null ? NO_VERSION : version, objectMapper.writeValueAsString(offers),
                    String.valueOf(cellTtl.toSeconds()));
        } catch (RuntimeException | JsonProcessingException exception) {
            failed("put", exception);
        }
    }

    @Override
    public void evict(Set<String> geohashes) {
        if (geohashes.isEmpty()) {
            return;
        }
        try {
            redis.executePipelined((RedisCallback<Object>) connection -> {
                var commands = (StringRedisConnection) connection;
                for (var cell : geohashes) {
                    commands.incr(versionKey(cell));
                    commands.expire(versionKey(cell), VERSION_TTL_SECONDS);
                    commands.del(cellKey(cell));
                }
                return null;
            });
            LOGGER.info("catalog.cell.cache.evicted cells={}", geohashes.size());
        } catch (RuntimeException exception) {
            // The cells expire by themselves within the TTL.
            failed("evict", exception);
        }
    }

    private boolean isSkipping() {
        if (skipping && nanoClock.getAsLong() - skipUntilNanos >= 0) {
            skipping = false;
            LOGGER.info("catalog.cell.cache.resumed");
        }
        return skipping;
    }

    private void failed(String operation, Exception exception) {
        failures.increment();
        skipUntilNanos = nanoClock.getAsLong() + pauseAfterFailure.toNanos();
        skipping = true;
        LOGGER.warn("catalog.cell.cache.failed operation={} reason={}", operation,
                exception.getClass().getSimpleName());
    }

    static String cellKey(String geohash) {
        return KEY_PREFIX + geohash;
    }

    static String versionKey(String geohash) {
        return KEY_PREFIX + geohash + VERSION_SUFFIX;
    }
}
