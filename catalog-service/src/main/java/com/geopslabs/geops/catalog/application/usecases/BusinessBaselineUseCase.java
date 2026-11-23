package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;

import java.util.Optional;

/** Módulo de Métricas: the business declares its usual numbers before GeoPS and reads them back (US45). */
public interface BusinessBaselineUseCase {
    BusinessBaseline declare(DeclareBaselineCommand command);

    Optional<BusinessBaseline> find(Long businessId);
}
