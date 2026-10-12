package com.geopslabs.geops.identity.domain.models;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * A SUNAT taxpayer number: 11 digits, a type prefix (10 natural person, 15 and 16 other natural persons, 17 natural
 * person with no residence, 20 company) and a check digit computed with the SUNAT weights.
 */
public record Ruc(String number) {
    public static final int RUC_LENGTH = 11;
    private static final Pattern DIGITS = Pattern.compile("\\d{" + RUC_LENGTH + "}");
    private static final Set<String> PREFIXES = Set.of("10", "15", "16", "17", "20");
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final int MODULUS = 11;

    public Ruc {
        number = number != null ? number.strip() : null;
    }

    public boolean isWellFormed() {
        return number != null && DIGITS.matcher(number).matches() && PREFIXES.contains(number.substring(0, 2))
                && checkDigitOf(number) == Character.digit(number.charAt(RUC_LENGTH - 1), 10);
    }

    /** 11 minus the weighted sum modulo 11; a result of 10 becomes 0 and 11 becomes 1. */
    static int checkDigitOf(String digits) {
        var sum = 0;
        for (int position = 0; position < WEIGHTS.length; position++) {
            sum += Character.digit(digits.charAt(position), 10) * WEIGHTS[position];
        }
        var result = MODULUS - sum % MODULUS;
        return result >= 10 ? result - 10 : result;
    }
}
