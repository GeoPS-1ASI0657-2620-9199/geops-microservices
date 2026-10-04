package com.geopslabs.geops.reservation.acceptance;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ScenarioScope
public class ScenarioState {
    private final List<HttpScenario.HttpResult> concurrentResults = new ArrayList<>();
    private Long reservationId;
    private String reservationCode;

    public void rememberReservation(Long id, String code) {
        this.reservationId = id;
        this.reservationCode = code;
    }

    public Long reservationId() {
        return reservationId;
    }

    public String reservationCode() {
        return reservationCode;
    }

    public List<HttpScenario.HttpResult> concurrentResults() {
        return concurrentResults;
    }
}
