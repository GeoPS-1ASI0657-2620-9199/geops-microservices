package com.geopslabs.geops.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface NearbyOfferRow {
    Long getOfferId();

    String getTitle();

    BigDecimal getPrice();

    LocalDate getValidTo();

    String getCategory();

    Double getLatitude();

    Double getLongitude();

    Long getBusinessId();

    String getBusinessName();

    Boolean getVerifiedSeal();

    Integer getOpenReports();
}
