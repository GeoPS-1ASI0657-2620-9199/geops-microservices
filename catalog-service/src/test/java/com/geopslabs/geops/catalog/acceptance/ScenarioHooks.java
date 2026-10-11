package com.geopslabs.geops.catalog.acceptance;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Every scenario starts with an empty catalog, an empty cell cache and the clock at its start: the cache would
 * otherwise answer a search with the cells of the previous scenario.
 */
public class ScenarioHooks {
    private static final String[] CLEAN_UP = {
            "DELETE FROM standing_changes",
            "DELETE FROM processed_events",
            "DELETE FROM moderation_actions",
            "DELETE FROM offers",
            "DELETE FROM campaign_daily_metrics",
            "DELETE FROM campaigns",
            "DELETE FROM business_baselines",
            "DELETE FROM merchant_standings"
    };

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final MutableClock clock;

    public ScenarioHooks(JdbcTemplate jdbc, StringRedisTemplate redis, MutableClock clock) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.clock = clock;
    }

    @Before(order = 0)
    public void startFromScratch() {
        clock.reset();
        for (var statement : CLEAN_UP) {
            jdbc.update(statement);
        }
        redis.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @After(order = 0)
    public void bringTheCacheBack() {
        RedisControl.resumeDatabase();
        RedisControl.resume();
    }
}
