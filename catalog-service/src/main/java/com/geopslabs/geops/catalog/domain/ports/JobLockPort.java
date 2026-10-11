package com.geopslabs.geops.catalog.domain.ports;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A lock that only one replica of Catalog holds at a time. A replica that dies never keeps it: the database
 * releases it with the transaction or the connection.
 */
public interface JobLockPort {
    /** Must run inside a transaction and is released when it ends; false when another replica holds the lock. */
    boolean tryLock(String jobName);

    /**
     * Runs the job holding the lock for as long as it takes, outside any transaction, so the job can commit its work
     * step by step. Empty when another replica holds the lock.
     */
    <T> Optional<T> runExclusively(String jobName, Supplier<T> job);
}
