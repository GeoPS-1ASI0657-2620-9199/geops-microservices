package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.ports.EmailDigestRepositoryPort;
import java.time.*;

public class EmailDigestService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final EmailDigestRepositoryPort digests;
    private final Duration window;
    public EmailDigestService(EmailDigestRepositoryPort digests, Duration window) {
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("Email window must be positive");
        this.digests = digests; this.window = window;
    }
    public void addToCurrentWindow(Long consumerId, Long offerId, Instant now) {
        var day = now.atZone(LIMA).toLocalDate().atStartOfDay(LIMA).toInstant();
        var number = Duration.between(day, now).toMillis() / window.toMillis();
        var start = day.plus(window.multipliedBy(number));
        digests.add(consumerId, offerId, start, start.plus(window));
    }
}
