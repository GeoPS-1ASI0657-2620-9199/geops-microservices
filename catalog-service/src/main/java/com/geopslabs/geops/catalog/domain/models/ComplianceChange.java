package com.geopslabs.geops.catalog.domain.models;

/**
 * What one event of Reservation adds to the compliance of a business (BRD-13, RF-03, S3-09). An answered report goes
 * to review and stops counting as open; a resolved report counts for the index only when the consumer was right.
 */
public record ComplianceChange(int openReportsDelta, int redeemedDelta, int consumerFavorDelta) {
    public static final String OPEN = "OPEN";
    public static final String CONSUMER_FAVOR = "CONSUMER_FAVOR";
    private static final int NONE = 0;
    private static final int ONE = 1;

    public static ComplianceChange forRedemption() {
        return new ComplianceChange(NONE, ONE, NONE);
    }

    public static ComplianceChange forReportOpened() {
        return new ComplianceChange(ONE, NONE, NONE);
    }

    public static ComplianceChange forReportAnswered() {
        return new ComplianceChange(-ONE, NONE, NONE);
    }

    public static ComplianceChange forReportResolved(String previousStatus, String resolution) {
        var stillOpen = OPEN.equals(previousStatus) ? -ONE : NONE;
        var consumerWasRight = CONSUMER_FAVOR.equals(resolution) ? ONE : NONE;
        return new ComplianceChange(stillOpen, NONE, consumerWasRight);
    }
}
