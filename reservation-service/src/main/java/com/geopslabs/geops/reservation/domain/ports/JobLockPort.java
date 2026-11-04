package com.geopslabs.geops.reservation.domain.ports;

/** A lock that only one replica can hold at a time, released when the surrounding transaction ends. */
public interface JobLockPort {
    boolean tryLock(String name);
}
