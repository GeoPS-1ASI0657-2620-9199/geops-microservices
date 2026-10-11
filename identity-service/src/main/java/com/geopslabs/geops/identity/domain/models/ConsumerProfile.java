package com.geopslabs.geops.identity.domain.models;

public class ConsumerProfile {
    private static final int DEFAULT_SEARCH_RADIUS_MINUTES = 10;
    public static final int MIN_SEARCH_RADIUS_MINUTES = 5;
    public static final int MAX_SEARCH_RADIUS_MINUTES = 20;

    private Long id;
    private Long userId;
    private boolean locationPermission;
    private int searchRadiusMinutes;
    private String defaultDistrict;

    public ConsumerProfile(Long userId, Boolean locationPermission, Integer searchRadiusMinutes,
                           String defaultDistrict) {
        this.userId = userId;
        this.locationPermission = Boolean.TRUE.equals(locationPermission);
        this.searchRadiusMinutes = searchRadiusMinutes != null ? searchRadiusMinutes : DEFAULT_SEARCH_RADIUS_MINUTES;
        this.defaultDistrict = defaultDistrict;
    }

    public ConsumerProfile(Long id, ConsumerProfile data) {
        this(data.userId, data.locationPermission, data.searchRadiusMinutes, data.defaultDistrict);
        this.id = id;
    }

    public static ConsumerProfile createFor(Long userId) {
        return new ConsumerProfile(userId, false, DEFAULT_SEARCH_RADIUS_MINUTES, null);
    }

    /**
     * Saves the walking radius the consumer chose (US46), from 5 to 20 minutes. Says whether it changed, so saving
     * the same value does not publish ConsumerPreferencesChanged again.
     */
    public boolean changeSearchRadius(int minutes) {
        if (minutes < MIN_SEARCH_RADIUS_MINUTES || minutes > MAX_SEARCH_RADIUS_MINUTES) {
            throw new SearchRadiusOutOfRangeException();
        }
        var changed = searchRadiusMinutes != minutes;
        searchRadiusMinutes = minutes;
        return changed;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isLocationPermission() {
        return locationPermission;
    }

    public int getSearchRadiusMinutes() {
        return searchRadiusMinutes;
    }

    public String getDefaultDistrict() {
        return defaultDistrict;
    }
}
