package com.geopslabs.geops.catalog.domain.ports;

/**
 * A lock that only one replica of Catalog holds at a time. It is released when the caller's transaction ends, so a
 * replica that dies never keeps it.
 */
public interface JobLockPort {
    /** Must run inside a transaction; false when another replica holds the lock. */
    boolean tryLock(String jobName);
}
