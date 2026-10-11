package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.VerifyBusinessRucCommand;
import com.geopslabs.geops.identity.domain.events.BusinessVerified;
import com.geopslabs.geops.identity.domain.events.DomainEvent;
import com.geopslabs.geops.identity.domain.models.AccountStatus;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.BusinessProfileAccessDeniedException;
import com.geopslabs.geops.identity.domain.models.BusinessProfileNotFoundException;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.GeocodingResult;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryEmptyException;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.models.VerificationResult;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.GeocodingPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.TaxpayerRegistryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessVerificationServiceTest {
    private static final Long BUSINESS_ID = 84L;
    private static final Long OTHER_BUSINESS_ID = 99L;
    private static final Ruc RUC = new Ruc("20123456786");
    private static final String ADDRESS = "Av. Larco 345, Miraflores";
    private static final Instant NOW = Instant.parse("2026-10-21T15:04:00Z");
    private static final UUID EVENT_ID = UUID.fromString("0c7a4c55-31e2-4f0f-9f0f-7a9a7f3b2d10");
    private static final GeoPoint LARCO = new GeoPoint(-12.1211, -77.0297);

    @Mock
    private BusinessProfileRepositoryPort businessProfiles;
    @Mock
    private RucVerificationRepositoryPort verifications;
    @Mock
    private TaxpayerRegistryPort taxpayerRegistry;
    @Mock
    private GeocodingPort geocoding;
    @Mock
    private OutboxPort outbox;

    private BusinessVerificationService service;

    @BeforeEach
    void setUp() {
        service = new BusinessVerificationService(businessProfiles, verifications, taxpayerRegistry, geocoding,
                outbox, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID);
        lenient().when(taxpayerRegistry.isLoaded()).thenReturn(true);
        lenient().when(verifications.save(any())).thenAnswer(invocation ->
                invocation.<RucVerification>getArgument(0).withId(31L));
        lenient().when(businessProfiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvesVerifiesTheProfileAndWritesBusinessVerified() {
        givenProfile(VerificationStatus.UNVERIFIED);
        givenTaxpayer("ACTIVO", "HABIDO");
        when(geocoding.geocode(ADDRESS)).thenReturn(GeocodingResult.found(LARCO));

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verification().result()).isEqualTo(VerificationResult.APPROVED);
        assertThat(outcome.verification().id()).isEqualTo(31L);
        assertThat(outcome.verification().checkedAt()).isEqualTo(NOW);
        assertThat(outcome.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        var event = publishedEvent();
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.aggregateId()).isEqualTo(BUSINESS_ID);
        assertThat(event.payload()).isEqualTo(new BusinessVerified.Payload(BUSINESS_ID, "Restaurante Don Pepe",
                true, VerificationResult.APPROVED, null, NOW));
    }

    @ParameterizedTest
    @CsvSource({
            "BAJA DE OFICIO, HABIDO, TAXPAYER_NOT_ACTIVE",
            "ACTIVO, NO HABIDO, TAXPAYER_NOT_HABIDO"
    })
    void rejectsATaxpayerThatIsNotActiveOrNotHabidoWithoutGeocoding(String status, String condition,
                                                                  VerificationReason reason) {
        givenProfile(VerificationStatus.UNVERIFIED);
        givenTaxpayer(status, condition);

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verification().reason()).isEqualTo(reason);
        assertThat(outcome.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        verify(geocoding, never()).geocode(anyString());
        verify(outbox, never()).append(any());
    }

    @Test
    void rejectsARucThatIsNotInTheRegistry() {
        givenProfile(VerificationStatus.UNVERIFIED);
        when(taxpayerRegistry.findByRuc(RUC)).thenReturn(Optional.empty());

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verification().reason()).isEqualTo(VerificationReason.RUC_NOT_FOUND);
        verify(outbox, never()).append(any());
    }

    @Test
    void anAddressThatCannotBeGeocodedIsRejectedAndNothingIsPublished() {
        givenProfile(VerificationStatus.UNVERIFIED);
        givenTaxpayer("ACTIVO", "HABIDO");
        when(geocoding.geocode(ADDRESS)).thenReturn(GeocodingResult.notFound());

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verification().reason()).isEqualTo(VerificationReason.ADDRESS_NOT_GEOCODABLE);
        verify(outbox, never()).append(any());
    }

    @Test
    void aGeocoderThatDoesNotAnswerLeavesThePendingStatusWithoutSeal() {
        givenProfile(VerificationStatus.UNVERIFIED);
        givenTaxpayer("ACTIVO", "HABIDO");
        when(geocoding.geocode(ADDRESS)).thenReturn(GeocodingResult.unavailable());

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verification().result()).isEqualTo(VerificationResult.PENDING);
        assertThat(outcome.verification().reason()).isEqualTo(VerificationReason.GEOCODER_UNAVAILABLE);
        assertThat(outcome.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        verify(outbox, never()).append(any());
    }

    @Test
    void aVerifiedBusinessWhoseRucIsNoLongerActiveLosesTheSealAndItIsPublished() {
        givenProfile(VerificationStatus.VERIFIED);
        givenTaxpayer("BAJA DE OFICIO", "HABIDO");

        var outcome = service.verify(ownerRequest(null));

        assertThat(outcome.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        var payload = (BusinessVerified.Payload) publishedEvent().payload();
        assertThat(payload.verified()).isFalse();
        assertThat(payload.reason()).isEqualTo(VerificationReason.TAXPAYER_NOT_ACTIVE);
    }

    @Test
    void theCorrectedAddressIsGeocodedAndSavedEvenIfItIsRejected() {
        givenProfile(VerificationStatus.REJECTED);
        givenTaxpayer("ACTIVO", "HABIDO");
        when(geocoding.geocode("Av. José Larco 345, Miraflores")).thenReturn(GeocodingResult.notFound());

        var outcome = service.verify(ownerRequest("Av. José Larco 345, Miraflores"));

        var saved = ArgumentCaptor.forClass(BusinessProfile.class);
        verify(businessProfiles).save(saved.capture());
        assertThat(saved.getValue().getAddress()).isEqualTo("Av. José Larco 345, Miraflores");
        assertThat(outcome.checkedAddress()).isEqualTo("Av. José Larco 345, Miraflores");
    }

    @Test
    void anotherBusinessCannotRequestTheVerification() {
        assertThatThrownBy(() -> service.verify(new VerifyBusinessRucCommand(BUSINESS_ID, OTHER_BUSINESS_ID, null)))
                .isInstanceOf(BusinessProfileAccessDeniedException.class);
        verify(businessProfiles, never()).findById(any());
    }

    @Test
    void anUnknownProfileIsNotFound() {
        when(businessProfiles.findById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(ownerRequest(null)))
                .isInstanceOf(BusinessProfileNotFoundException.class);
    }

    @Test
    void anEmptyRegistryAnswersUnavailableInsteadOfRejectingEveryRuc() {
        givenProfile(VerificationStatus.UNVERIFIED);
        when(taxpayerRegistry.isLoaded()).thenReturn(false);

        assertThatThrownBy(() -> service.verify(ownerRequest(null)))
                .isInstanceOf(TaxpayerRegistryEmptyException.class);
        verify(verifications, never()).save(any());
    }

    @Test
    void theRetryVerifiesAPendingBusinessAgain() {
        givenProfile(VerificationStatus.PENDING);
        givenTaxpayer("ACTIVO", "HABIDO");
        when(geocoding.geocode(ADDRESS)).thenReturn(GeocodingResult.found(LARCO));
        when(businessProfiles.findIdsByVerificationStatus(VerificationStatus.PENDING)).thenReturn(List.of(BUSINESS_ID));

        assertThat(service.pendingBusinessIds()).containsExactly(BUSINESS_ID);
        var outcome = service.retry(BUSINESS_ID);

        assertThat(outcome.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(outbox).append(any());
    }

    private void givenProfile(VerificationStatus status) {
        var data = BusinessProfile.register("Restaurante Don Pepe", "Restaurante", RUC, ADDRESS,
                new GeoPoint(-12.12, -77.03), null);
        when(businessProfiles.findById(BUSINESS_ID))
                .thenReturn(Optional.of(new BusinessProfile(BUSINESS_ID, 42L, data, AccountStatus.ACTIVE, status)));
    }

    private void givenTaxpayer(String status, String condition) {
        when(taxpayerRegistry.findByRuc(RUC))
                .thenReturn(Optional.of(new Taxpayer(RUC, "RESTAURANTE DON PEPE S.A.C.", status, condition)));
    }

    private static VerifyBusinessRucCommand ownerRequest(String address) {
        return new VerifyBusinessRucCommand(BUSINESS_ID, BUSINESS_ID, address);
    }

    private DomainEvent publishedEvent() {
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        return event.getValue();
    }
}
