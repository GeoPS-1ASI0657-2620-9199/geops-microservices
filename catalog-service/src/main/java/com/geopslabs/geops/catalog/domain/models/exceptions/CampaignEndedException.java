package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.ConflictException;

import java.time.LocalDate;

public class CampaignEndedException extends ConflictException {
    private static final String MESSAGE = "La campaña terminó el %s. Ya no se puede pausar ni reactivar.";

    public CampaignEndedException(LocalDate end) {
        super(CampaignAlreadyEndedException.CODE, MESSAGE.formatted(end));
    }
}
