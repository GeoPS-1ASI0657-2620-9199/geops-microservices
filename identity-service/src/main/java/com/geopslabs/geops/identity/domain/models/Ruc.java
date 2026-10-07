package com.geopslabs.geops.identity.domain.models;

import java.util.regex.Pattern;

public record Ruc(String number) {
    public static final int RUC_LENGTH = 11;
    private static final Pattern DIGITS = Pattern.compile("\\d{" + RUC_LENGTH + "}");

    public Ruc {
        number = number != null ? number.strip() : null;
    }

    public boolean isWellFormed() {
        return number != null && DIGITS.matcher(number).matches();
    }
}
