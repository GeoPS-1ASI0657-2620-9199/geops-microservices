package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.ports.JobLockPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** pg_try_advisory_xact_lock: held until the transaction ends, so a crashed replica never keeps it. */
@Component
public class PostgresJobLockAdapter implements JobLockPort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean tryLock(String name) {
        return (Boolean) entityManager.createNativeQuery("SELECT pg_try_advisory_xact_lock(hashtext(:name))")
                .setParameter("name", name).getSingleResult();
    }
}
