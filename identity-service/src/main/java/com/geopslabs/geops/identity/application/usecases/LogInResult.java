package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.IssuedToken;
import com.geopslabs.geops.identity.domain.models.Role;

public record LogInResult(IssuedToken token, Long userId, Role role, Long consumerId,
                          Long businessId, String businessName) {
}
