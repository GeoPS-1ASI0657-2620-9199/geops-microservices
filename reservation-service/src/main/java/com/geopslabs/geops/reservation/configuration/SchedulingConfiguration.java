package com.geopslabs.geops.reservation.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "geops.reports.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfiguration {
}
