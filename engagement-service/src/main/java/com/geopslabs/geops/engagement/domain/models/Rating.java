package com.geopslabs.geops.engagement.domain.models;

public record Rating(Integer stars) {
    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;
    private static final String OUT_OF_RANGE_MESSAGE = "A rating has from %d to %d stars";

    public Rating {
        if (stars == null || stars < MIN_STARS || stars > MAX_STARS) {
            throw new IllegalArgumentException(OUT_OF_RANGE_MESSAGE.formatted(MIN_STARS, MAX_STARS));
        }
    }
}
