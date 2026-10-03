package com.geopslabs.geops.identity.domain.models;

public class ConsumerProfile {
    private static final int DEFAULT_SEARCH_RADIUS_MINUTES = 10;

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

    public void updateConsumerDetails(Boolean locationPermission, Integer searchRadiusMinutes,
                                      String defaultDistrict) {
        this.locationPermission = locationPermission != null ? locationPermission : this.locationPermission;
        this.searchRadiusMinutes = searchRadiusMinutes != null ? searchRadiusMinutes : this.searchRadiusMinutes;
        this.defaultDistrict = defaultDistrict != null ? defaultDistrict : this.defaultDistrict;
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
