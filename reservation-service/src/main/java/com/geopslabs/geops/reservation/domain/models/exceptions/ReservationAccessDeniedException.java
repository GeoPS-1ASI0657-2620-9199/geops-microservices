package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ForbiddenException;

public class ReservationAccessDeniedException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String OTHER_CONSUMER_MESSAGE = "Reservation %d belongs to another account";
    private static final String OTHER_BUSINESS_MESSAGE = "This code belongs to another business";

    private ReservationAccessDeniedException(String message) {
        super(CODE, message);
    }

    /** The consumer asked by id, so the id is already theirs to see. */
    public static ReservationAccessDeniedException otherConsumer(Long reservationId) {
        return new ReservationAccessDeniedException(OTHER_CONSUMER_MESSAGE.formatted(reservationId));
    }

    /** The business only typed a code; the answer says nothing about the reservation behind it. */
    public static ReservationAccessDeniedException otherBusiness() {
        return new ReservationAccessDeniedException(OTHER_BUSINESS_MESSAGE);
    }
}
