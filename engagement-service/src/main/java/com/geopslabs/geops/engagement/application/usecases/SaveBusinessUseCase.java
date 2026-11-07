package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
public interface SaveBusinessUseCase {
    Result save(Long consumerId, Long businessId);
    record Result(SavedBusiness business, boolean created) {}
}
