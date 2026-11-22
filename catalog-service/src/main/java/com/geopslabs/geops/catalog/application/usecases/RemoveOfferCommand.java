package com.geopslabs.geops.catalog.application.usecases;

public record RemoveOfferCommand(Long offerId, Long adminId, String reason) {
}
