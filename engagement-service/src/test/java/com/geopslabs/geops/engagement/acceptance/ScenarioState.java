package com.geopslabs.geops.engagement.acceptance;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioState {
    private Long savedOfferId;

    public void rememberSavedOffer(Long id) {
        this.savedOfferId = id;
    }

    public Long savedOfferId() {
        return savedOfferId;
    }
}
