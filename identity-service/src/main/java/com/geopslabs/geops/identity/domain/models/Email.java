package com.geopslabs.geops.identity.domain.models;

import java.util.Locale;
import java.util.Objects;

public record Email(String value) {
    public Email {
        value = Objects.requireNonNull(value).strip().toLowerCase(Locale.ROOT);
    }
}
