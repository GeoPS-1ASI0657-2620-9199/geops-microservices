package com.geopslabs.geops.notification.acceptance;

import com.geopslabs.geops.notification.domain.models.Recipient;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.jdbc.core.JdbcTemplate;

public class RecipientSteps {
    private static final String CLEAN_TABLES = """
            TRUNCATE TABLE last_known_locations, push_subscriptions, email_unsubscriptions, notification_preferences,
                notifications, email_digests, followed_businesses, recipients RESTART IDENTITY CASCADE""";

    private final JdbcTemplate jdbcTemplate;
    private final RecipientRepositoryPort recipients;

    public RecipientSteps(JdbcTemplate jdbcTemplate, RecipientRepositoryPort recipients) {
        this.jdbcTemplate = jdbcTemplate;
        this.recipients = recipients;
    }

    @Before
    public void startWithEmptyTables() {
        jdbcTemplate.execute(CLEAN_TABLES);
    }

    @Given("consumer {long} has the recipient copy {string} with a confirmed email")
    public void consumerHasRecipientCopy(Long consumerId, String email) {
        recipients.upsert(new Recipient(consumerId, email, Boolean.TRUE, Recipient.CONSUMER));
    }
}
