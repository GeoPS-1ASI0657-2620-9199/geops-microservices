package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

import java.time.LocalDate;

public class CampaignAlreadyEndedException extends DomainException {
    public static final String CODE = "CAMPAIGN_ALREADY_ENDED";
    private static final String MESSAGE = "La vigencia de la campaña terminó el %s. Elige una fecha de fin desde hoy.";

    public CampaignAlreadyEndedException(LocalDate end) {
        super(CODE, MESSAGE.formatted(end));
    }
}
