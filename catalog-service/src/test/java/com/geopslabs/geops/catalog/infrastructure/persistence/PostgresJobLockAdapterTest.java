package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.acceptance.CucumberSpringConfiguration;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two replicas are two connections to the same PostgreSQL: while one holds a job, the other one is told no.
 */
class PostgresJobLockAdapterTest extends CucumberSpringConfiguration {
    private static final String JOB = "catalog.test-job";
    private static final long WAIT_SECONDS = 10;

    @Autowired
    private JobLockPort locks;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void aJobHeldByOneReplicaIsRefusedToTheOther() {
        var nested = locks.runExclusively(JOB, () -> locks.runExclusively(JOB, () -> "second"));

        assertThat(nested).contains(Optional.empty());
        assertThat(locks.runExclusively(JOB, () -> "after")).contains("after");
    }

    @Test
    void theTransactionLockIsHeldUntilTheCommit() throws Exception {
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var first = CompletableFuture.supplyAsync(() -> newTransaction().execute(status -> {
            var locked = locks.tryLock(JOB);
            held.countDown();
            await(release);
            return locked;
        }));
        assertThat(held.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        Boolean second = newTransaction().execute(status -> locks.tryLock(JOB));
        release.countDown();

        Boolean firstLocked = first.get(WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(firstLocked).isTrue();
        assertThat(second).isFalse();
        Boolean third = newTransaction().execute(status -> locks.tryLock(JOB));
        assertThat(third).isTrue();
    }

    private TransactionTemplate newTransaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
