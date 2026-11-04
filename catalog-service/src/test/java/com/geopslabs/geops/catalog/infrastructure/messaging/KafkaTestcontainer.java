package com.geopslabs.geops.catalog.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;

/**
 * A broker with the same image as platform/docker-compose.yml and the topics this service reads, each with its
 * dead letter topic. Import it in a listener test: {@code @Import(KafkaTestcontainer.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestcontainer {
    private static final String[] TOPICS = {"identity.events", "catalog.events", "reservation.events", "engagement.events"};

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka:3.7.0");
    }

    @Bean
    org.springframework.kafka.core.KafkaAdmin.NewTopics eventTopics() {
        var topics = new java.util.ArrayList<NewTopic>();
        for (var topic : TOPICS) {
            topics.add(new NewTopic(topic, 3, (short) 1));
            topics.add(new NewTopic(topic + ".DLT", 3, (short) 1));
        }
        return new org.springframework.kafka.core.KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }
}
