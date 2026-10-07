package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MerchantStanding {
    public static final BigDecimal INITIAL_COMPLIANCE_INDEX = new BigDecimal("100.00");
    private static final int NO_OPEN_REPORTS = 0;

    private final Long businessId;
    private final String businessName;
    private final boolean rucVerified;
    private final int openReports;
    private final BigDecimal complianceIndex;
    private final boolean verifiedSeal;
    private final LocalDateTime updatedAt;

    public MerchantStanding(Long businessId, String businessName, boolean rucVerified, int openReports,
                            BigDecimal complianceIndex, boolean verifiedSeal, LocalDateTime updatedAt) {
        this.businessId = businessId;
        this.businessName = businessName;
        this.rucVerified = rucVerified;
        this.openReports = openReports;
        this.complianceIndex = complianceIndex;
        this.verifiedSeal = verifiedSeal;
        this.updatedAt = updatedAt;
    }

    public static MerchantStanding provisional(Long businessId, String businessName, LocalDateTime createdAt) {
        return new MerchantStanding(businessId, businessName, false, NO_OPEN_REPORTS, INITIAL_COMPLIANCE_INDEX,
                false, createdAt);
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public boolean isRucVerified() {
        return rucVerified;
    }

    public int getOpenReports() {
        return openReports;
    }

    public BigDecimal getComplianceIndex() {
        return complianceIndex;
    }

    public boolean hasVerifiedSeal() {
        return verifiedSeal;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
