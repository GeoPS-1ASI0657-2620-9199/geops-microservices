package com.geopslabs.geops.engagement.acceptance;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import io.cucumber.java.en.*;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

public class ReviewReplySteps {
    private final HttpScenario http;
    private final ReviewRepositoryPort reviews;
    private final RedeemedReservationRepositoryPort redemptions;
    private Long reviewId;
    public ReviewReplySteps(HttpScenario http, ReviewRepositoryPort reviews, RedeemedReservationRepositoryPort redemptions) {
        this.http = http; this.reviews = reviews; this.redemptions = redemptions;
    }

    @Given("consumer {long} published a review of business {long} with rating {int} and text {string}")
    public void review(Long consumerId, Long businessId, int rating, String text) {
        var redemption = new RedeemedReservation(5012L, consumerId, businessId, LocalDateTime.of(2026, 10, 7, 12, 0));
        redemptions.saveIfAbsent(redemption);
        reviewId = reviews.save(Review.of(redemption, new Rating(rating), text, LocalDateTime.of(2026, 10, 8, 12, 0))).getId();
    }

    @When("the business owner of business {long} replies {string}")
    public void reply(Long businessId, String text) throws Exception {
        http.post("/api/v1/reviews/" + reviewId + "/replies", TestIdentity.businessOwnerToken(9L, businessId),
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("text", text)));
    }

    @Then("the reviews of business {long} show {string} with the reply {string}")
    public void shown(Long businessId, String text, String reply) {
        assertThat(reviews.findByBusinessId(businessId)).singleElement().satisfies(r -> {
            assertThat(r.getText()).isEqualTo(text);
            assertThat(r.getReply().text()).isEqualTo(reply);
        });
    }
}
