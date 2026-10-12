package com.geopslabs.geops.identity.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The registry load, the retry of pending verifications and the outbox relay run on Spring's scheduler. Its pool
 * size (spring.task.scheduling.pool.size) keeps a long registry load from holding back the other tasks.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
