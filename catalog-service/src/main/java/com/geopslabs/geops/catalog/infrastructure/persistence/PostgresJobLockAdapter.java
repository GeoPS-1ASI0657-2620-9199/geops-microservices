package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL transaction-level advisory lock: pg_try_advisory_xact_lock never waits and is released at commit or
 * rollback. Two replicas running the same job at the same time get true in one and false in the other.
 */
@Component
public class PostgresJobLockAdapter implements JobLockPort {
    private static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(hashtext(:name))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean tryLock(String jobName) {
        var locked = entityManager.createNativeQuery(TRY_LOCK).setParameter("name", jobName).getSingleResult();
        return Boolean.TRUE.equals(locked);
    }
}
