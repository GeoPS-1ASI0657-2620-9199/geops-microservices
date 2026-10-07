package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchRadiusException;

public record WalkingRadius(int minutes) {
    public static final int MIN_WALK_MINUTES = 5;
    public static final int MAX_WALK_MINUTES = 20;
    public static final int METERS_PER_WALK_MINUTE = 80;

    public WalkingRadius {
        if (minutes < MIN_WALK_MINUTES || minutes > MAX_WALK_MINUTES) {
            throw new InvalidSearchRadiusException(MIN_WALK_MINUTES, MAX_WALK_MINUTES);
        }
    }

    public int toMeters() {
        return minutes * METERS_PER_WALK_MINUTE;
    }

    public static int walkMinutesFor(double distanceMeters) {
        return (int) Math.ceil(distanceMeters / METERS_PER_WALK_MINUTE);
    }
}
