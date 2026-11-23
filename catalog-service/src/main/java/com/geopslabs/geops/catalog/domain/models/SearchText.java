package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchTextException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Text typed in the search bar. It matches the title of the offer, the name of the business or the category,
 * without telling upper from lower case or accented from plain letters ("cafe" finds "Café"). It is applied in
 * memory over the candidates of the radius, like the category, so the cell cache keeps serving whole cells.
 */
public final class SearchText {
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 60;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final String folded;

    private SearchText(String folded) {
        this.folded = folded;
    }

    /** Null or blank means no text filter; otherwise the trimmed text must have 2 to 60 characters. */
    public static SearchText parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var trimmed = SPACES.matcher(raw.strip()).replaceAll(" ");
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new InvalidSearchTextException(MIN_LENGTH, MAX_LENGTH);
        }
        return new SearchText(fold(trimmed));
    }

    public boolean matches(NearbyOfferCandidate candidate) {
        return contains(candidate.title()) || contains(candidate.businessName()) || contains(candidate.category());
    }

    public String value() {
        return folded;
    }

    private boolean contains(String field) {
        return field != null && fold(field).contains(folded);
    }

    static String fold(String text) {
        var withoutMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return SPACES.matcher(withoutMarks.toLowerCase(Locale.ROOT)).replaceAll(" ");
    }
}
