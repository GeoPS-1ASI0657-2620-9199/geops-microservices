package com.geopslabs.geops.catalog.acceptance;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioState {
    private final Map<String, Long> campaignIds = new HashMap<>();
    private final Map<String, Long> offerIds = new HashMap<>();

    public void rememberCampaign(String name, Long campaignId) {
        campaignIds.put(name, campaignId);
    }

    public void rememberOffer(String title, Long offerId) {
        offerIds.put(title, offerId);
    }

    public Long campaignId(String name) {
        return campaignIds.get(name);
    }

    public Long offerId(String title) {
        return offerIds.get(title);
    }
}
