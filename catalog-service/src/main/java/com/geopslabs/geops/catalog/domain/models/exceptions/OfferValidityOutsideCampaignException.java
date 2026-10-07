package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class OfferValidityOutsideCampaignException extends DomainException {
    public static final String CODE = "OFFER_VALIDITY_OUTSIDE_CAMPAIGN";
    private static final String MESSAGE = "La vigencia de la oferta «%s» debe estar dentro del periodo de la campaña.";

    public OfferValidityOutsideCampaignException(String title) {
        super(CODE, MESSAGE.formatted(title));
    }
}
