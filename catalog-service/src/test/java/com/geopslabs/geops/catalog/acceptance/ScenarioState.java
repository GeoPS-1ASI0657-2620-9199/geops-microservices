package com.geopslabs.geops.catalog.acceptance;

import com.geopslabs.geops.catalog.domain.models.Offer;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioState {
    private final Map<String, Long> campaignIds = new HashMap<>();
    private final Map<String, Long> offerIds = new HashMap<>();
    private final Map<Long, Long> campaignIdsByBusiness = new HashMap<>();
    private Offer currentOffer;

    public void rememberCampaign(String name, Long campaignId) {
        campaignIds.put(name, campaignId);
    }

    public void rememberOffer(String title, Long offerId) {
        offerIds.put(title, offerId);
    }

    public void rememberCampaignOfBusiness(Long businessId, Long campaignId) {
        campaignIdsByBusiness.put(businessId, campaignId);
    }

    public void rememberCurrentOffer(Offer offer) {
        currentOffer = offer;
    }

    public Long campaignId(String name) {
        return campaignIds.get(name);
    }

    public Long offerId(String title) {
        return offerIds.get(title);
    }

    public Long campaignIdOfBusiness(Long businessId) {
        return campaignIdsByBusiness.get(businessId);
    }

    public Offer currentOffer() {
        return currentOffer;
    }
}
