package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedOfferQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Long CONSUMER_ID = 1L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long VALID_OFFER_ID = 1L;
    private static final Long EXPIRED_OFFER_ID = 2L;
    private static final String BUSINESS_NAME = "Bodega Doña Rosa";
    private static final LocalDateTime NEWER_SAVE = LocalDateTime.parse("2026-10-08T13:00:00");
    private static final LocalDateTime OLDER_SAVE = LocalDateTime.parse("2026-09-28T15:40:00");

    @Mock
    private SavedOfferRepositoryPort savedOfferRepository;
    @Mock
    private OfferSnapshotRepositoryPort offerSnapshotRepository;
    @Mock
    private BusinessSnapshotRepositoryPort businessSnapshotRepository;

    private SavedOfferQueryService service;

    @BeforeEach
    void setUp() {
        service = new SavedOfferQueryService(savedOfferRepository, offerSnapshotRepository,
                businessSnapshotRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(businessSnapshotRepository.findById(BUSINESS_ID))
                .thenReturn(Optional.of(new BusinessSnapshot(BUSINESS_ID, BUSINESS_NAME)));
    }

    @Test
    void listsOnlyTheSavedOffersOfTheConsumerNewestFirst() {
        givenSavedOffers();

        var views = service.list(new GetSavedOffersByConsumerQuery(CONSUMER_ID));

        verify(savedOfferRepository).findByConsumerId(CONSUMER_ID);
        assertThat(views).extracting(view -> view.offer().offerId()).containsExactly(VALID_OFFER_ID, EXPIRED_OFFER_ID);
        assertThat(views).extracting(view -> view.savedOffer().getSavedAt()).containsExactly(NEWER_SAVE, OLDER_SAVE);
        assertThat(views).allMatch(view -> BUSINESS_NAME.equals(view.businessName()));
    }

    @Test
    void marksTheOfferThatExpiredAfterBeingSaved() {
        givenSavedOffers();

        var views = service.list(new GetSavedOffersByConsumerQuery(CONSUMER_ID));

        assertThat(views).extracting(view -> view.expired()).containsExactly(false, true);
    }

    private void givenSavedOffers() {
        when(savedOfferRepository.findByConsumerId(CONSUMER_ID)).thenReturn(List.of(
                new SavedOffer(9L, CONSUMER_ID, VALID_OFFER_ID, NEWER_SAVE),
                new SavedOffer(7L, CONSUMER_ID, EXPIRED_OFFER_ID, OLDER_SAVE)));
        when(offerSnapshotRepository.findById(VALID_OFFER_ID)).thenReturn(Optional.of(new OfferSnapshot(
                VALID_OFFER_ID, BUSINESS_ID, "Menú ejecutivo a mitad de precio", LocalDate.parse("2026-10-31"),
                OfferSnapshot.PUBLISHED)));
        when(offerSnapshotRepository.findById(EXPIRED_OFFER_ID)).thenReturn(Optional.of(new OfferSnapshot(
                EXPIRED_OFFER_ID, BUSINESS_ID, "Desayuno criollo a S/ 8", LocalDate.parse("2026-10-01"),
                OfferSnapshot.PUBLISHED)));
    }
}
