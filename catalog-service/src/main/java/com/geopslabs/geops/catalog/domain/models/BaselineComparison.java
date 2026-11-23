package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;

/**
 * The campaign measured against the usual numbers of the business (US45). expectedCustomers is what the business
 * serves in the period without GeoPS; customersChangePercent is the redemptions of the campaign as a percentage of
 * it, that is, how much the campaign added on top of the usual customers; estimatedExtraSales prices those
 * customers at the declared average ticket.
 */
public record BaselineComparison(BigDecimal expectedCustomers, long campaignCustomers,
                                 BigDecimal customersChangePercent, BigDecimal estimatedExtraSales) {
}
