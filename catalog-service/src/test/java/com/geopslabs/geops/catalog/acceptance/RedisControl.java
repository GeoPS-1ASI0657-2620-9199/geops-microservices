package com.geopslabs.geops.catalog.acceptance;

/**
 * Pauses and resumes the Redis and PostgreSQL containers. A paused container keeps its port, so the client times out exactly as
 * with a Redis that stopped answering, and the rest of the suite gets it back.
 */
final class RedisControl {
    private static volatile boolean paused;
    private static volatile boolean databasePaused;

    private RedisControl() {
    }

    static synchronized void pause() {
        var redis = CucumberSpringConfiguration.REDIS;
        redis.getDockerClient().pauseContainerCmd(redis.getContainerId()).exec();
        paused = true;
    }

    static synchronized void resume() {
        if (!paused) {
            return;
        }
        var redis = CucumberSpringConfiguration.REDIS;
        redis.getDockerClient().unpauseContainerCmd(redis.getContainerId()).exec();
        paused = false;
    }

    /** The database stops answering: open connections time out (socketTimeout) and new ones are refused. */
    static synchronized void pauseDatabase() {
        var postgres = CucumberSpringConfiguration.POSTGRES;
        postgres.getDockerClient().pauseContainerCmd(postgres.getContainerId()).exec();
        databasePaused = true;
    }

    static synchronized void resumeDatabase() {
        if (!databasePaused) {
            return;
        }
        var postgres = CucumberSpringConfiguration.POSTGRES;
        postgres.getDockerClient().unpauseContainerCmd(postgres.getContainerId()).exec();
        databasePaused = false;
    }
}
