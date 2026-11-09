package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ForbiddenException;

public class ReportAccessDeniedException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String OTHER_PARTY_MESSAGE = "Report %d belongs to another account";
    private static final String NOT_ALLOWED_MESSAGE = "Only the administrator can resolve a report in favor of the business";

    private ReportAccessDeniedException(String message) {
        super(CODE, message);
    }

    public static ReportAccessDeniedException otherParty(Long reportId) {
        return new ReportAccessDeniedException(OTHER_PARTY_MESSAGE.formatted(reportId));
    }

    public static ReportAccessDeniedException businessFavorIsForTheAdministrator() {
        return new ReportAccessDeniedException(NOT_ALLOWED_MESSAGE);
    }
}
