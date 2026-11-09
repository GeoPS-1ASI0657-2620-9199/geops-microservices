package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Catalog's local copy of the reputation of a business (Módulo de Reputación del Comercio). Identity owns the RUC
 * verification; Catalog keeps the last answer it heard, with the instant it happened, and derives the seal from it
 * (CON06): verified RUC and a compliance index of at least 90 %. The index comes from Reservation (US44): redeemed
 * reservations over redeemed reservations plus reports closed in favor of the consumer.
 */
public class MerchantStanding {
    public static final BigDecimal INITIAL_COMPLIANCE_INDEX = new BigDecimal("100.00");
    public static final BigDecimal SEAL_COMPLIANCE_THRESHOLD = new BigDecimal("90.00");
    public static final Instant NEVER_UPDATED = Instant.EPOCH;
    private static final int NO_OPEN_REPORTS = 0;
    private static final int NO_CASES = 0;
    private static final int INDEX_SCALE = 2;
    private static final long FULL_PERCENT = 100L;

    private final Long businessId;
    private final String businessName;
    private final boolean rucVerified;
    private final int openReports;
    private final BigDecimal complianceIndex;
    private final boolean verifiedSeal;
    private final Instant updatedAt;
    private final int redeemedReservations;
    private final int consumerFavorReports;

    public MerchantStanding(Long businessId, String businessName, boolean rucVerified, int openReports,
                            BigDecimal complianceIndex, boolean verifiedSeal, Instant updatedAt) {
        this(businessId, businessName, rucVerified, openReports, complianceIndex, verifiedSeal, updatedAt, NO_CASES,
                NO_CASES);
    }

    @SuppressWarnings("java:S107")
    public MerchantStanding(Long businessId, String businessName, boolean rucVerified, int openReports,
                            BigDecimal complianceIndex, boolean verifiedSeal, Instant updatedAt,
                            int redeemedReservations, int consumerFavorReports) {
        this.businessId = businessId;
        this.businessName = businessName;
        this.rucVerified = rucVerified;
        this.openReports = openReports;
        this.complianceIndex = complianceIndex;
        this.verifiedSeal = verifiedSeal;
        this.updatedAt = updatedAt;
        this.redeemedReservations = redeemedReservations;
        this.consumerFavorReports = consumerFavorReports;
    }

    /**
     * The standing of a business that publishes before Catalog heard anything about it. Its updatedAt is the
     * beginning of time, not the moment of the campaign: updatedAt is when the last reputation fact happened, and a
     * BusinessVerified that happened before the first campaign but arrives later must still apply.
     */
    public static MerchantStanding provisional(Long businessId, String businessName) {
        return new MerchantStanding(businessId, businessName, false, NO_OPEN_REPORTS, INITIAL_COMPLIANCE_INDEX,
                false, NEVER_UPDATED);
    }

    public static boolean sealFor(boolean rucVerified, BigDecimal complianceIndex) {
        return rucVerified && complianceIndex != null && complianceIndex.compareTo(SEAL_COMPLIANCE_THRESHOLD) >= 0;
    }

    /** A business that verified before publishing any campaign: the standing starts from the event. */
    public static MerchantStanding fromRucVerification(Long businessId, String businessName, boolean verified,
                                                       Instant at) {
        return new MerchantStanding(businessId, businessName, verified, NO_OPEN_REPORTS, INITIAL_COMPLIANCE_INDEX,
                sealFor(verified, INITIAL_COMPLIANCE_INDEX), at);
    }

    /** Identity said the RUC is verified or not at the given instant; the seal is recalculated. */
    public MerchantStanding applyRucVerification(boolean verified, String name, Instant at) {
        var newName = name == null || name.isBlank() ? businessName : name.strip();
        return new MerchantStanding(businessId, newName, verified, openReports, complianceIndex,
                sealFor(verified, complianceIndex), at, redeemedReservations, consumerFavorReports);
    }

    /**
     * Reservation reported a redemption or a change of a report. Counters only add up, so the order of the events
     * does not matter and updatedAt is left as it is: it marks the last RUC answer, and moving it would make a late
     * BusinessVerified look old. open_reports never goes below zero.
     */
    public MerchantStanding applyCompliance(ComplianceChange change) {
        var open = Math.max(NO_OPEN_REPORTS, openReports + change.openReportsDelta());
        var redeemed = redeemedReservations + change.redeemedDelta();
        var consumerFavor = consumerFavorReports + change.consumerFavorDelta();
        var index = complianceIndexOf(redeemed, consumerFavor);
        return new MerchantStanding(businessId, businessName, rucVerified, open, index, sealFor(rucVerified, index),
                updatedAt, redeemed, consumerFavor);
    }

    /** 100 × redeemed / (redeemed + consumer favor), 100.00 with no cases, two decimals rounded half up. */
    public static BigDecimal complianceIndexOf(int redeemed, int consumerFavor) {
        var cases = redeemed + consumerFavor;
        if (cases == NO_CASES) {
            return INITIAL_COMPLIANCE_INDEX;
        }
        return BigDecimal.valueOf(FULL_PERCENT * redeemed)
                .divide(BigDecimal.valueOf(cases), INDEX_SCALE, RoundingMode.HALF_UP);
    }

    /** The position of the business in a search depends on its open reports and its seal. */
    public boolean ranksDifferentlyFrom(MerchantStanding other) {
        return openReports != other.openReports || verifiedSeal != other.verifiedSeal;
    }

    /** True when the change described happened before the last one this copy already holds. */
    public boolean isNewerThan(Instant instant) {
        return updatedAt != null && updatedAt.isAfter(instant);
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getRedeemedReservations() {
        return redeemedReservations;
    }

    public int getConsumerFavorReports() {
        return consumerFavorReports;
    }
}
