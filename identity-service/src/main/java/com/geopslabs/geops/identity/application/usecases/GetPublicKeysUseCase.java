package com.geopslabs.geops.identity.application.usecases;

import java.util.Map;

public interface GetPublicKeysUseCase {
    Map<String, Object> publicKeys();
}
