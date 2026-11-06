package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavedBusinessCommandServiceTest {
    private final SavedBusinessRepositoryPort saved = mock(SavedBusinessRepositoryPort.class);
    private final BusinessSnapshotRepositoryPort businesses = mock(BusinessSnapshotRepositoryPort.class);
    private final SavedBusinessCommandService service = new SavedBusinessCommandService(saved, businesses, Clock.systemUTC());
    @Test
    void savesKnownBusinessesAndReusesExistingEntries() {
        when(businesses.findById(84L)).thenReturn(Optional.of(new BusinessSnapshot(84L, "Don Pepe")));
        when(saved.exists(2001L, 84L)).thenReturn(false, true);
        assertThat(service.save(2001L, 84L).created()).isTrue();
        assertThat(service.save(2001L, 84L).created()).isFalse();
    }
    @Test
    void missingBusinessIsNotSavedAndRemovalIsScopedToTheConsumer() {
        assertThatThrownBy(() -> service.save(2001L, 99L)).isInstanceOf(BusinessNotFoundException.class);
        verify(saved, never()).save(any(), any(), any());
        assertThatThrownBy(() -> service.remove(2001L, 84L)).isInstanceOf(SavedBusinessNotFoundException.class);
        verify(saved).delete(2001L, 84L);
    }
    @Test
    void pausedOffersCannotBeSavedAsAvailable() {
        var offer = new OfferSnapshot(1052L, 84L, "2x1", LocalDate.parse("2026-11-15"), OfferSnapshot.PAUSED);
        assertThat(offer.isAvailable(LocalDate.parse("2026-10-27"))).isFalse();
        assertThat(offer.isExpired(LocalDate.parse("2026-10-27"))).isFalse();
    }
}
