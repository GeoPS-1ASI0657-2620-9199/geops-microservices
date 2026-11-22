package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.DeclareBaselineCommand;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidBaselineException;
import com.geopslabs.geops.catalog.domain.ports.BusinessBaselineRepositoryPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessBaselineServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-01T15:20:00Z");
    private static final Long BUSINESS_ID = 84L;

    @Mock
    private BusinessBaselineRepositoryPort baselines;

    private BusinessBaselineService service;

    @BeforeEach
    void setUp() {
        service = new BusinessBaselineService(baselines, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsTheNumbersWithTheDateOfTheDeclaration() {
        when(baselines.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.declare(new DeclareBaselineCommand(BUSINESS_ID, 140, new BigDecimal("18.50")));

        assertThat(saved).isEqualTo(new BusinessBaseline(BUSINESS_ID, 140, new BigDecimal("18.50"), NOW));
        verify(baselines).save(saved);
    }

    @Test
    void aBusinessThatJustRegisteredDeclaresWithoutItsLocalCopy() {
        when(baselines.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.declare(new DeclareBaselineCommand(66L, 40, new BigDecimal("12.00")));

        assertThat(saved).isEqualTo(new BusinessBaseline(66L, 40, new BigDecimal("12.00"), NOW));
        verify(baselines).save(saved);
    }

    @Test
    void theDeclaredBaselineIsReadBack() {
        var stored = new BusinessBaseline(BUSINESS_ID, 140, new BigDecimal("18.50"), NOW);
        when(baselines.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(stored));

        assertThat(service.find(BUSINESS_ID)).contains(stored);
    }

    @Test
    void invalidNumbersAreRejectedBeforeReadingAnything() {
        assertThatThrownBy(() -> service.declare(new DeclareBaselineCommand(BUSINESS_ID, 0, BigDecimal.TEN)))
                .isInstanceOf(InvalidBaselineException.class);
        verifyNoInteractions(baselines);
    }
}
