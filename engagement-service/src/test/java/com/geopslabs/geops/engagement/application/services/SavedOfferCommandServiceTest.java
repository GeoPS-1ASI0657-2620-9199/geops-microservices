package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.engagement.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.models.exceptions.SavedOfferNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedOfferCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final LocalDateTime SAVED_AT = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Long CONSUMER_ID = 1L;
    private static final Long OFFER_ID = 1L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long SAVED_OFFER_ID = 9L;
    private static final String TITLE = "Menú ejecutivo a mitad de precio";
    private static final String BUSINESS_NAME = "Bodega Doña Rosa";
    private static final LocalDate VALID_TO = LocalDate.parse("2026-10-31");
    private static final LocalDate YESTERDAY_IN_LIMA = LocalDate.parse("2026-10-07");
    private static final long ONE_REMOVED = 1L;
    private static final long NONE_REMOVED = 0L;
    private static final SaveOfferCommand COMMAND = new SaveOfferCommand(CONSUMER_ID, OFFER_ID);

    @Mock
    private SavedOfferRepositoryPort savedOfferRepository;
    @Mock
    private OfferSnapshotRepositoryPort offerSnapshotRepository;
    @Mock
    private BusinessSnapshotRepositoryPort businessSnapshotRepository;

    private SavedOfferCommandService service;

    @BeforeEach
    void setUp() {
        service = new SavedOfferCommandService(savedOfferRepository, offerSnapshotRepository,
                businessSnapshotRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void savesAValidOfferWithTheSnapshotData() {
        givenOfferValidTo(VALID_TO, OfferSnapshot.PUBLISHED);
        givenBusiness();
        when(savedOfferRepository.findByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID)).thenReturn(Optional.empty());
        when(savedOfferRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var result = service.save(COMMAND);

        assertThat(result.created()).isTrue();
        assertThat(result.savedOffer().savedOffer().getId()).isEqualTo(SAVED_OFFER_ID);
        assertThat(result.savedOffer().savedOffer().getSavedAt()).isEqualTo(SAVED_AT);
        assertThat(result.savedOffer().offer().title()).isEqualTo(TITLE);
        assertThat(result.savedOffer().businessName()).isEqualTo(BUSINESS_NAME);
        assertThat(result.savedOffer().expired()).isFalse();
    }

    @Test
    void offerWithoutSnapshotIsNotFound() {
        when(offerSnapshotRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(COMMAND)).isInstanceOf(OfferNotFoundException.class);
        verify(savedOfferRepository, never()).save(any());
    }

    @Test
    void expiredOfferIsNotAvailableAndIsNotSaved() {
        givenOfferValidTo(YESTERDAY_IN_LIMA, OfferSnapshot.PUBLISHED);

        assertThatThrownBy(() -> service.save(COMMAND)).isInstanceOf(OfferNotAvailableException.class);
        verify(savedOfferRepository, never()).save(any());
    }

    @Test
    void savingTheSameOfferAgainReturnsTheExistingOneWithoutInserting() {
        givenOfferValidTo(VALID_TO, OfferSnapshot.PUBLISHED);
        givenBusiness();
        var existing = storedSavedOffer();
        when(savedOfferRepository.findByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID)).thenReturn(Optional.of(existing));

        var result = service.save(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.savedOffer().savedOffer()).isSameAs(existing);
        verify(savedOfferRepository, never()).save(any());
    }

    @Test
    void aClashWithTheUniqueKeyReturnsTheRowOfTheConcurrentRequest() {
        givenOfferValidTo(VALID_TO, OfferSnapshot.PUBLISHED);
        givenBusiness();
        var concurrent = storedSavedOffer();
        when(savedOfferRepository.findByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(savedOfferRepository.save(any())).thenThrow(new SavedOfferAlreadyExistsException());

        var result = service.save(COMMAND);

        assertThat(result.created()).isFalse();
        assertThat(result.savedOffer().savedOffer()).isSameAs(concurrent);
    }

    @Test
    void removesASavedOffer() {
        when(savedOfferRepository.deleteByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID)).thenReturn(ONE_REMOVED);

        service.remove(new RemoveSavedOfferCommand(CONSUMER_ID, OFFER_ID));

        verify(savedOfferRepository).deleteByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID);
    }

    @Test
    void removingAnOfferThatIsNotSavedIsNotFound() {
        when(savedOfferRepository.deleteByConsumerIdAndOfferId(CONSUMER_ID, OFFER_ID)).thenReturn(NONE_REMOVED);

        assertThatThrownBy(() -> service.remove(new RemoveSavedOfferCommand(CONSUMER_ID, OFFER_ID)))
                .isInstanceOf(SavedOfferNotFoundException.class);
    }

    private void givenOfferValidTo(LocalDate validTo, String status) {
        when(offerSnapshotRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(new OfferSnapshot(OFFER_ID, BUSINESS_ID, TITLE, validTo, status)));
    }

    private void givenBusiness() {
        when(businessSnapshotRepository.findById(BUSINESS_ID))
                .thenReturn(Optional.of(new BusinessSnapshot(BUSINESS_ID, BUSINESS_NAME)));
    }

    private static SavedOffer withId(SavedOffer savedOffer) {
        return new SavedOffer(SAVED_OFFER_ID, savedOffer.getConsumerId(), savedOffer.getOfferId(),
                savedOffer.getSavedAt());
    }

    private static SavedOffer storedSavedOffer() {
        return new SavedOffer(SAVED_OFFER_ID, CONSUMER_ID, OFFER_ID, SAVED_AT);
    }
}
