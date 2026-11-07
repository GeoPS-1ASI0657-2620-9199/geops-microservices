package com.geopslabs.geops.notification.domain.ports;

public interface RecipientProjectionPort {
    void updateRadius(Long consumerId, int radiusMeters);
    void follow(Long consumerId, Long businessId);
    void unfollow(Long consumerId, Long businessId);
}
