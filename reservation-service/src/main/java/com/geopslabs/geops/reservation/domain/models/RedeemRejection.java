package com.geopslabs.geops.reservation.domain.models;

/**
 * Why a redemption did not change the reservation, in the order the business reads them.
 */
public enum RedeemRejection {
    NOT_FOUND,
    OTHER_BUSINESS,
    ALREADY_REDEEMED,
    REPORTED,
    EXPIRED
}
