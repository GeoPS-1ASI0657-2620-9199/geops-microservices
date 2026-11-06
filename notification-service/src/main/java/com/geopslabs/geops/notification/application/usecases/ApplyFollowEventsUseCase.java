package com.geopslabs.geops.notification.application.usecases;

public interface ApplyFollowEventsUseCase {
    void businessFollowed(Long consumerId, Long businessId);
    void businessUnfollowed(Long consumerId, Long businessId);
}
