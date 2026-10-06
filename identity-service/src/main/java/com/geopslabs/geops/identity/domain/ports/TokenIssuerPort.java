package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.IssuedToken;
import com.geopslabs.geops.identity.domain.models.User;

import java.util.Map;

public interface TokenIssuerPort {
    IssuedToken issue(User user, Long profileId);

    Map<String, Object> publicKeys();
}
