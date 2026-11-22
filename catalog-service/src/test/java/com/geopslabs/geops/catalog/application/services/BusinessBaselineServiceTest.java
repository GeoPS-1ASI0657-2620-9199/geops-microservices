package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.DeclareBaselineCommand;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.models.exceptions.BusinessNotFoundException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidBaselineException;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessBaselineServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-01T15:20:00Z");
    private static final Long BUSINESS_ID = 84L;

    @Mock
    private BusinessBaselineRepositoryPort baselines;
    @Mock
    private MerchantStandingRepositoryPort standings;

    private BusinessBaselineService service;

    @BeforeEach
    void setUp() {
        service = new BusinessBaselineService(baselines, standings, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsTheNumbersWithTheDateOfTheDeclaration() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing()));
        when(baselines.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.declare(new DeclareBaselineCommand(BUSINESS_ID, 140, new BigDecimal("18.50")));

        assertThat(saved).isEqualTo(new BusinessBaseline(BUSINESS_ID, 140, new BigDecimal("18.50"), NOW));
        verify(baselines).save(saved);
    }

    @Test
    void aBusinessCatalogDoesNotKnowCannotDeclare() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.declare(new DeclareBaselineCommand(BUSINESS_ID, 140, BigDecimal.TEN)))
                .isInstanceOf(BusinessNotFoundException.class);
        verify(baselines, never()).save(any());
    }

    @Test
    void invalidNumbersAreRejectedBeforeReadingAnything() {
        assertThatThrownBy(() -> service.declare(new DeclareBaselineCommand(BUSINESS_ID, 0, BigDecimal.TEN)))
                .isInstanceOf(InvalidBaselineException.class);
        verifyNoInteractions(standings, baselines);
    }

    private static MerchantStanding standing() {
        return new MerchantStanding(BUSINESS_ID, "Restaurante Don Pepe", true, 0, new BigDecimal("100.00"), true,
                NOW);
    }
}
