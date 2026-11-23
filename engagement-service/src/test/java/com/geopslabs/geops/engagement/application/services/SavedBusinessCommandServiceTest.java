package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavedBusinessCommandServiceTest {
    private final SavedBusinessRepositoryPort saved = mock(SavedBusinessRepositoryPort.class);
    private final BusinessSnapshotRepositoryPort businesses = mock(BusinessSnapshotRepositoryPort.class);
    private final OfferSnapshotRepositoryPort offers = mock(OfferSnapshotRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneOffset.UTC);
    private final SavedBusinessCommandService service = new SavedBusinessCommandService(saved, businesses, offers, clock);
    @Test
    void savesKnownBusinessesAndReusesExistingEntries() {
        when(businesses.findById(84L)).thenReturn(Optional.of(new BusinessSnapshot(84L, "Don Pepe")));
        var business = new SavedBusiness(1L, 2001L, 84L, "Don Pepe", Instant.now());
        when(saved.saveIfAbsent(eq(2001L), eq(84L), any())).thenReturn(Optional.of(business), Optional.empty());
        when(saved.findByConsumerAndBusiness(2001L, 84L)).thenReturn(Optional.of(business));
        assertThat(service.save(2001L, 84L).created()).isTrue();
        assertThat(service.save(2001L, 84L).created()).isFalse();
    }
    @Test
    void missingBusinessIsNotSavedAndRemovalIsScopedToTheConsumer() {
        assertThatThrownBy(() -> service.save(2001L, 99L)).isInstanceOf(BusinessNotFoundException.class);
        verify(saved, never()).saveIfAbsent(any(), any(), any());
        assertThatThrownBy(() -> service.remove(2001L, 84L)).isInstanceOf(SavedBusinessNotFoundException.class);
        verify(saved).delete(2001L, 84L);
    }
    @Test
    void theListGivesEveryOpenOfferOfEachSavedBusinessSoTheConsumerPicksOne() {
        var savedAt = Instant.parse("2026-11-20T10:00:00Z");
        when(saved.findByConsumer(2001L)).thenReturn(List.of(new SavedBusiness(1L, 2001L, 84L, "Don Pepe", savedAt),
                new SavedBusiness(2L, 2001L, 85L, "Bodega Rosa", savedAt)));
        var menu = new OfferSnapshot(1052L, 84L, "Menu del dia", LocalDate.parse("2026-11-30"), OfferSnapshot.PUBLISHED);
        var twoForOne = new OfferSnapshot(1053L, 84L, "2x1", LocalDate.parse("2026-12-05"), OfferSnapshot.PUBLISHED);
        when(offers.findAvailableByBusinessIds(List.of(84L), LocalDate.parse("2026-11-23"))).thenReturn(List.of(menu, twoForOne));
        when(offers.findAvailableByBusinessIds(List.of(85L), LocalDate.parse("2026-11-23"))).thenReturn(List.of());

        var list = service.list(2001L);

        assertThat(list).extracting(view -> view.activeOffersCount()).containsExactly(2, 0);
        assertThat(list.get(0).activeOffers()).containsExactly(menu, twoForOne);
        assertThat(list.get(1).activeOffers()).isEmpty();
    }
    @Test
    void pausedOffersCannotBeSavedAsAvailable() {
        var offer = new OfferSnapshot(1052L, 84L, "2x1", LocalDate.parse("2026-11-15"), OfferSnapshot.PAUSED);
        assertThat(offer.isAvailable(LocalDate.parse("2026-10-27"))).isFalse();
        assertThat(offer.isExpired(LocalDate.parse("2026-10-27"))).isFalse();
    }
}
