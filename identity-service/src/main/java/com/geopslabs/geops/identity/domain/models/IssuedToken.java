package com.geopslabs.geops.identity.domain.models;

import java.time.Duration;

public record IssuedToken(String value, Duration lifetime) {
}
