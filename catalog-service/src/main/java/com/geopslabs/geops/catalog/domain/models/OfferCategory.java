package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCategoryException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Closed list of offer categories (E2-06). A category is compared without accents, case or surrounding spaces and
 * always kept with its canonical label, so the search can filter by equality.
 */
public record OfferCategory(String label) {
    public static final List<String> LABELS = List.of(
            "Gastronomía", "Cafetería y panadería", "Bodega y minimarket", "Salud y farmacia",
            "Belleza y cuidado personal", "Deporte", "Mascotas", "Hogar y ferretería", "Librería y útiles",
            "Servicios", "Cultura y entretenimiento");
    private static final Map<String, String> LABEL_BY_KEY = LABELS.stream()
            .collect(Collectors.toUnmodifiableMap(OfferCategory::keyOf, Function.identity()));

    public OfferCategory {
        if (label == null || !LABELS.contains(label)) {
            throw new InvalidCategoryException(LABELS);
        }
    }

    public static OfferCategory of(String text) {
        var label = text == null ? null : LABEL_BY_KEY.get(keyOf(text));
        if (label == null) {
            throw new InvalidCategoryException(LABELS);
        }
        return new OfferCategory(label);
    }

    /** Null when no category was asked for; the canonical label otherwise. */
    public static String labelOrNull(String text) {
        return text == null || text.isBlank() ? null : of(text).label();
    }

    private static String keyOf(String text) {
        var withoutAccents = Normalizer.normalize(text.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
