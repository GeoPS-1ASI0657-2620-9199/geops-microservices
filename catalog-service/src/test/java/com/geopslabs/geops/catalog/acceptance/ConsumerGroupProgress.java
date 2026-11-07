package com.geopslabs.geops.catalog.acceptance;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Waits until Catalog's consumer group has committed every record written to a topic, which happens only after the
 * event was applied and its cells deleted.
 */
final class ConsumerGroupProgress {
    private static final String GROUP = "catalog-service";
    private static final Duration WAIT = Duration.ofSeconds(90);
    private static final long POLL_MILLIS = 100;

    private ConsumerGroupProgress() {
    }

    static void awaitConsumed(KafkaAdmin kafkaAdmin, String topic) throws Exception {
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var partitions = admin.describeTopics(List.of(topic)).allTopicNames().get().get(topic)
                    .partitions().stream().map(info -> new TopicPartition(topic, info.partition())).toList();
            var deadline = Instant.now().plus(WAIT);
            while (Instant.now().isBefore(deadline)) {
                if (allCommitted(admin, partitions)) {
                    return;
                }
                Thread.sleep(POLL_MILLIS);
            }
        }
        throw new AssertionError("Catalog did not consume " + topic + " within " + WAIT);
    }

    private static boolean allCommitted(AdminClient admin, List<TopicPartition> partitions) throws Exception {
        var ends = admin.listOffsets(partitions.stream()
                        .collect(Collectors.toMap(Function.identity(), partition -> OffsetSpec.latest())))
                .all().get();
        Map<TopicPartition, Long> committed = admin.listConsumerGroupOffsets(GROUP)
                .partitionsToOffsetAndMetadata().get().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));
        return partitions.stream().allMatch(partition ->
                committed.getOrDefault(partition, 0L) >= ends.get(partition).offset());
    }
}
