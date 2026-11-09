package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class SearchRadiusOutOfRangeException extends RuleViolationException {
    private static final String CODE = "RADIUS_OUT_OF_RANGE";
    private static final String MESSAGE = "Elige un radio entre %d y %d minutos a pie.";

    public SearchRadiusOutOfRangeException() {
        super(CODE, MESSAGE.formatted(ConsumerProfile.MIN_SEARCH_RADIUS_MINUTES,
                ConsumerProfile.MAX_SEARCH_RADIUS_MINUTES));
    }
}
