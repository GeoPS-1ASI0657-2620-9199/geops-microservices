package com.geopslabs.geops.catalog.acceptance;

/**
 * Pauses and resumes the Redis container. A paused container keeps its port, so the client times out exactly as
 * with a Redis that stopped answering, and the rest of the suite gets it back.
 */
final class RedisControl {
    private static volatile boolean paused;

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
}
