package com.geopslabs.geops.engagement.domain.models;

import java.util.Locale;

/**
 * The name a business sees next to a review: the first name and the initial of the next word ("Lucía F."), so the
 * review never exposes the full surname. It is a snapshot taken when the review is published.
 */
public final class AuthorName {
    public static final int MAX_LENGTH = 60;
    private static final String INITIAL_SUFFIX = ".";
    private static final int INITIAL_PART_LENGTH = 3;
    private static final int FIRST_NAME_MAX_LENGTH = MAX_LENGTH - INITIAL_PART_LENGTH;
    private static final String WHITESPACE = "\\s+";

    private AuthorName() {
    }

    /** Returns null when the full name is missing or blank, so the review keeps no name. */
    public static String shorten(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        var words = fullName.strip().split(WHITESPACE);
        var firstName = truncate(words[0]);
        if (words.length == 1) {
            return firstName;
        }
        return firstName + " " + initial(words[1]) + INITIAL_SUFFIX;
    }

    private static String truncate(String word) {
        if (word.codePointCount(0, word.length()) <= FIRST_NAME_MAX_LENGTH) {
            return word;
        }
        return word.substring(0, word.offsetByCodePoints(0, FIRST_NAME_MAX_LENGTH));
    }

    private static String initial(String word) {
        return word.substring(0, Character.charCount(word.codePointAt(0))).toUpperCase(Locale.ROOT);
    }
}
