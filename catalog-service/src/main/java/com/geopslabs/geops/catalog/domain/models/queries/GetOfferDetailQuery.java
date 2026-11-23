package com.geopslabs.geops.catalog.domain.models.queries;

/**
 * The public detail of an offer. countsAsClick is true only when the offer-detail screen opens it (US11); the
 * reservation screen and the rest of the web read the same detail without adding clicks.
 */
public record GetOfferDetailQuery(Long offerId, boolean countsAsClick) {
    public GetOfferDetailQuery(Long offerId) {
        this(offerId, false);
    }
}
