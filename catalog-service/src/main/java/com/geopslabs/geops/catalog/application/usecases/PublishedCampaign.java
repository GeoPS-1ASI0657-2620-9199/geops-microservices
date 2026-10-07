package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.util.List;

public record PublishedCampaign(Campaign campaign, List<Offer> offers) {
}
