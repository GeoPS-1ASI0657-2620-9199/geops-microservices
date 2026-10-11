package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

public interface GetConsumerProfileUseCase {
    ConsumerProfile get(Long consumerId);
}
