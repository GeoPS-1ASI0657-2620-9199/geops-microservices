package com.geopslabs.geops.notification.infrastructure.resilience;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTransitionLoggerTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(CircuitBreakerTransitionLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
    }

    @Test
    void logsEachStateChangeOfEveryCircuitBreakerOfTheRegistry() {
        var registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
                new CircuitBreakerTransitionLogger());
        var fcm = registry.circuitBreaker("fcm");

        fcm.transitionToOpenState();
        fcm.transitionToHalfOpenState();
        fcm.transitionToClosedState();

        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "circuit.opened name=fcm from=CLOSED",
                "circuit.half-opened name=fcm from=OPEN",
                "circuit.closed name=fcm from=HALF_OPEN");
    }
}
