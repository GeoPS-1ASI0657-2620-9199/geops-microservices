package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportReasonException;

/** The reason a consumer gives, 10 to 500 characters without blanks at the ends. */
public record ReportReason(String value) {
    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 500;

    public ReportReason {
        value = value == null ? "" : value.strip();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidReportReasonException(MIN_LENGTH, MAX_LENGTH);
        }
    }
}
