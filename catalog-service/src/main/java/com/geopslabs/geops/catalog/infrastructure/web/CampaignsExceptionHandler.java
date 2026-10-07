package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAlreadyEndedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignPeriodException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferValidityOutsideCampaignException;
import com.geopslabs.geops.catalog.shared.domain.DomainException;
import com.geopslabs.geops.catalog.shared.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CampaignsController.class)
public class CampaignsExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignsExceptionHandler.class);

    @ExceptionHandler({CampaignAlreadyEndedException.class, InvalidCampaignPeriodException.class,
            InvalidCampaignZoneException.class, OfferValidityOutsideCampaignException.class,
            InvalidGeoPointException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRejectedCampaign(DomainException exception) {
        LOGGER.info("campaign.rejected code={}", exception.getCode());
        return new ErrorResponse(exception.getCode(), exception.getMessage());
    }
}
