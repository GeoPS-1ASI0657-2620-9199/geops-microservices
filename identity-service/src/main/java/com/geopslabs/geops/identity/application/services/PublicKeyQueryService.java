package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.GetPublicKeysUseCase;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;

import java.util.Map;

public class PublicKeyQueryService implements GetPublicKeysUseCase {
    private final TokenIssuerPort tokenIssuer;

    public PublicKeyQueryService(TokenIssuerPort tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public Map<String, Object> publicKeys() {
        return tokenIssuer.publicKeys();
    }
}
