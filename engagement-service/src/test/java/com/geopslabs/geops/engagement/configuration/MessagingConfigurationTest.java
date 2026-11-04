package com.geopslabs.geops.engagement.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingConfigurationTest {

    @Test
    void aDatabaseOutageBehindTheListenerIsRetriedWithoutLimit() {
        var wrapped = new ListenerExecutionFailedException("listener",
                new CannotCreateTransactionException("no connection", new RuntimeException("refused")));

        assertThat(MessagingConfiguration.isInfrastructure(wrapped)).isTrue();
        assertThat(MessagingConfiguration.isInfrastructure(new DataAccessResourceFailureException("down"))).isTrue();
    }

    @Test
    void aRuleOrABugGoesThroughTheFiveAttemptsAndThenToTheDeadLetterTopic() {
        assertThat(MessagingConfiguration.isInfrastructure(new IllegalStateException("bug"))).isFalse();
        assertThat(MessagingConfiguration.isInfrastructure(new DuplicateKeyException("rule"))).isFalse();
    }
}
