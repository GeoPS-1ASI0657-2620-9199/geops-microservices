package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.OfferPublishedEvent;
public interface PlanOfferNoticesUseCase { void offerPublished(OfferPublishedEvent offer); }
