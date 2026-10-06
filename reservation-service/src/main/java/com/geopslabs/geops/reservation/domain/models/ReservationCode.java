package com.geopslabs.geops.reservation.domain.models;

import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record ReservationCode(String value) {
    public static final int CODE_LENGTH = 8;
    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String INVALID_CODE_MESSAGE = "A reservation code has %d characters from %s";

    public ReservationCode {
        if (!isWellFormed(value)) {
            throw new IllegalArgumentException(INVALID_CODE_MESSAGE.formatted(CODE_LENGTH, ALPHABET));
        }
    }

    public static ReservationCode generate(RandomGenerator random) {
        var value = IntStream.range(0, CODE_LENGTH)
                .mapToObj(position -> String.valueOf(ALPHABET.charAt(random.nextInt(ALPHABET.length()))))
                .collect(Collectors.joining());
        return new ReservationCode(value);
    }

    public static boolean isWellFormed(String value) {
        return value != null
                && value.length() == CODE_LENGTH
                && value.chars().allMatch(character -> ALPHABET.indexOf(character) >= 0);
    }

    @Override
    public String toString() {
        return value;
    }
}
