package com.geopslabs.geops.engagement.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.engagement.infrastructure.persistence.ReviewJpaRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewSteps {
    private static final String REVIEWS = "/api/v1/reviews";
    private static final String REVIEWS_OF_BUSINESS = REVIEWS + "?businessId=%d";
    private static final String REVIEW_BODY = "{\"businessId\": %d, \"rating\": %d, \"text\": \"%s\"}";
    private static final String PREVIOUS_REVIEW_TEXT = "Primera visita";
    private static final int PREVIOUS_REVIEW_STARS = 4;
    private static final int HTTP_CREATED = 201;

    private final HttpScenario http;
    private final ReviewJpaRepository reviews;

    public ReviewSteps(HttpScenario http, ReviewJpaRepository reviews) {
        this.http = http;
        this.reviews = reviews;
    }

    @Given("consumer {long} already reviewed business {long}")
    public void consumerAlreadyReviewed(Long consumerId, Long businessId) {
        var result = review(TestIdentity.consumerToken(consumerId), businessId, PREVIOUS_REVIEW_STARS,
                PREVIOUS_REVIEW_TEXT);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_CREATED);
    }

    @When("consumer {long} reviews business {long} with {int} stars and the text {string}")
    public void consumerReviews(Long consumerId, Long businessId, int stars, String text) {
        review(TestIdentity.consumerToken(consumerId), businessId, stars, text);
    }

    @When("business {long} is reviewed {} with {int} stars")
    public void businessIsReviewedWithToken(Long businessId, String tokenKind, int stars) {
        review(SavedOfferSteps.tokenOfKind(tokenKind), businessId, stars, PREVIOUS_REVIEW_TEXT);
    }

    @When("consumer {long} lists the reviews of business {long}")
    public void consumerListsReviews(Long consumerId, Long businessId) {
        http.get(REVIEWS_OF_BUSINESS.formatted(businessId), TestIdentity.consumerToken(consumerId));
    }

    @Then("the reviews come in this order: {string}")
    public void reviewsComeInOrder(String expectedTexts) throws JsonProcessingException {
        var texts = new ArrayList<String>();
        for (var item : http.json(http.last())) {
            texts.add(item.path("text").asText());
        }
        assertThat(texts).containsExactlyElementsOf(List.of(expectedTexts.split(", ")));
    }

    @And("no review was stored")
    public void noReviewWasStored() {
        assertThat(reviews.count()).isZero();
    }

    @And("{int} review(s) is/are stored for business {long}")
    public void reviewsAreStored(int expected, Long businessId) {
        assertThat(reviews.findByBusinessIdOrderByCreatedAtDescIdDesc(businessId)).hasSize(expected);
    }

    private HttpScenario.HttpResult review(String token, Long businessId, int stars, String text) {
        return http.post(REVIEWS, token, REVIEW_BODY.formatted(businessId, stars, text));
    }
}
