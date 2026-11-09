package com.geopslabs.geops.catalog.acceptance;

import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase;
import com.geopslabs.geops.catalog.application.usecases.BusinessVerifiedEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The real use case, but it can be told to fail the next calls with a NullPointerException, as a bug in Catalog
 * would. Lets a scenario check that such a failure is retried and never discards the event.
 */
public class FlakyBusinessVerified implements ApplyBusinessVerifiedUseCase {
    private final ApplyBusinessVerifiedUseCase real;
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();

    public FlakyBusinessVerified(ApplyBusinessVerifiedUseCase real) {
        this.real = real;
    }

    public void failNext(int times) {
        failuresLeft.set(times);
        calls.set(0);
    }

    public int calls() {
        return calls.get();
    }

    public void reset() {
        failuresLeft.set(0);
        calls.set(0);
    }

    @Override
    public Outcome apply(BusinessVerifiedEvent event) {
        calls.incrementAndGet();
        if (failuresLeft.getAndUpdate(left -> Math.max(0, left - 1)) > 0) {
            throw new NullPointerException("simulated bug while applying " + event.eventId());
        }
        return real.apply(event);
    }
}
