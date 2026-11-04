package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportAnswerException;

import java.time.LocalDateTime;

/** What the business replied. A report has at most one. */
public record ReportAnswer(Long id, String text, LocalDateTime respondedAt) {
    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 2000;

    public ReportAnswer {
        text = text == null ? "" : text.strip();
        if (text.length() < MIN_LENGTH || text.length() > MAX_LENGTH) {
            throw new InvalidReportAnswerException(MIN_LENGTH, MAX_LENGTH);
        }
    }
}
