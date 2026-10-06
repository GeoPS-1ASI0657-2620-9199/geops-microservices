package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.commands.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.DeleteOfferCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;

import java.util.Optional;

public interface OfferCommandUseCase {

    Optional<Offer> handle(CreateOfferCommand command);

    Optional<Offer> handle(UpdateOfferCommand command);

    boolean handle(DeleteOfferCommand command);
}
