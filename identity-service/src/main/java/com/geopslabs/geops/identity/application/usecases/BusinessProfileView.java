package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.RucVerification;

import java.util.Optional;

public record BusinessProfileView(BusinessProfile profile, Optional<RucVerification> latestVerification) {
}
