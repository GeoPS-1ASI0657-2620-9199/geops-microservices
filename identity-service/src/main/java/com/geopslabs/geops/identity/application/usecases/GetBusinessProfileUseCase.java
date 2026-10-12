package com.geopslabs.geops.identity.application.usecases;

public interface GetBusinessProfileUseCase {
    BusinessProfileView get(Long businessId, Long requesterBusinessId);
}
