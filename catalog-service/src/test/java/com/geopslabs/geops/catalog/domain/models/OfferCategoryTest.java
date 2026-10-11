package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCategoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferCategoryTest {

    @Test
    void theClosedListHasTheElevenCategoriesOfE206InOrder() {
        assertThat(OfferCategory.LABELS).hasSize(11)
                .startsWith("Gastronomía")
                .endsWith("Cultura y entretenimiento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Gastronomía", "gastronomia", "  GASTRONOMÍA ", "Gastronomia"})
    void findsTheCanonicalLabelWithoutAccentsCaseOrSpaces(String text) {
        assertThat(OfferCategory.of(text).label()).isEqualTo("Gastronomía");
    }

    @Test
    void rejectsACategoryOutsideTheListAndNamesTheValidOnes() {
        assertThatThrownBy(() -> OfferCategory.of("Juguetería"))
                .isInstanceOf(InvalidCategoryException.class)
                .hasMessageContaining("Mascotas")
                .extracting("code").isEqualTo("INVALID_CATEGORY");
    }

    @Test
    void anAbsentCategoryMeansEveryCategory() {
        assertThat(OfferCategory.labelOrNull(null)).isNull();
        assertThat(OfferCategory.labelOrNull(" ")).isNull();
        assertThat(OfferCategory.labelOrNull("mascotas")).isEqualTo("Mascotas");
    }

    @Test
    void rejectsANullCategoryWhenOneIsRequired() {
        assertThatThrownBy(() -> OfferCategory.of(null)).isInstanceOf(InvalidCategoryException.class);
    }
}
