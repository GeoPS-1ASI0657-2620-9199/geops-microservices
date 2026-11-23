package com.geopslabs.geops.reservation.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs each state change of a circuit breaker as circuit.opened, circuit.half-opened or circuit.closed with its
 * name, so an outage of a dependency and its recovery can be followed in the logs (US34).
 */
public class CircuitBreakerTransitionLogger implements RegistryEventConsumer<CircuitBreaker> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerTransitionLogger.class);

    @Override
    public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
        listen(event.getAddedEntry());
    }

    @Override
    public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
        // Nothing to release: the listener goes away with the circuit breaker.
    }

    @Override
    public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
        listen(event.getNewEntry());
    }

    private static void listen(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher().onStateTransition(CircuitBreakerTransitionLogger::log);
    }

    static void log(CircuitBreakerOnStateTransitionEvent event) {
        var name = event.getCircuitBreakerName();
        var from = event.getStateTransition().getFromState();
        var to = event.getStateTransition().getToState();
        switch (to) {
            case OPEN, FORCED_OPEN -> LOGGER.warn("circuit.opened name={} from={}", name, from);
            case HALF_OPEN -> LOGGER.info("circuit.half-opened name={} from={}", name, from);
            case CLOSED -> LOGGER.info("circuit.closed name={} from={}", name, from);
            default -> LOGGER.info("circuit.state-changed name={} from={} to={}", name, from, to);
        }
    }
}
