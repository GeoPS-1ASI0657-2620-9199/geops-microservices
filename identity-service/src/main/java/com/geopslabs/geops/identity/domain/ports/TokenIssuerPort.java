package com.geopslabs.geops.identity.domain.ports;

public interface TokenIssuerPort {
    String generateToken(String username);
}
