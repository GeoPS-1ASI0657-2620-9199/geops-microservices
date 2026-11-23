package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.ModerationFilter;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ResultPage;
import com.geopslabs.geops.catalog.domain.ports.ModerationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModerationQueryServiceTest {
    private final ModerationRepositoryPort repository = mock(ModerationRepositoryPort.class);
    private final ModerationQueryService service = new ModerationQueryService(repository);

    @Test
    void theOffersComeFromTheRepositoryWithTheFilter() {
        var filter = new ModerationFilter(Set.of(OfferStatus.PUBLISHED), true, " almuerzo ");
        var page = new ResultPage<>(List.of(), 1, 0, 0);
        when(repository.findOffers(filter, 1, 20)).thenReturn(new ResultPage<>(List.of(), 1, 0, 0));

        assertThat(service.listOffers(filter, 1, 20)).isEqualTo(page);
        assertThat(filter.text()).isEqualTo("almuerzo");
    }

    @Test
    void noStatusMeansEveryStatusAndABlankTextMeansNoText() {
        var filter = new ModerationFilter(null, false, "  ");

        assertThat(filter.statuses()).isEqualTo(EnumSet.allOf(OfferStatus.class));
        assertThat(filter.text()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 51"})
    void anInvalidPageIsRejected(int page, int size) {
        var filter = new ModerationFilter(null, false, null);

        assertThatThrownBy(() -> service.listHistory(page, size)).isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> service.listOffers(filter, page, size))
                .isInstanceOf(InvalidPageRequestException.class);
        verifyNoInteractions(repository);
    }
}
