package com.geopslabs.geops.engagement.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.engagement.infrastructure.persistence.SavedOfferJpaRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SavedOfferSteps {
    static final String SAVED_OFFERS = "/api/v1/saved-offers";
    private static final String SAVED_OFFER = SAVED_OFFERS + "/%d";
    private static final String OFFER_BODY = "{\"offerId\": %d}";
    private static final String EMPTY_BODY = "{}";
    private static final Long BUSINESS_OWNER_USER_ID = 9001L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long CONSUMER_ID = 1L;
    private static final int HTTP_CREATED = 201;

    private final HttpScenario http;
    private final ScenarioState state;
    private final SavedOfferJpaRepository savedOffers;

    public SavedOfferSteps(HttpScenario http, ScenarioState state, SavedOfferJpaRepository savedOffers) {
        this.http = http;
        this.state = state;
        this.savedOffers = savedOffers;
    }

    @Given("consumer {long} saved offer {long}")
    public void consumerSavedOffer(Long consumerId, Long offerId) throws JsonProcessingException {
        var result = save(TestIdentity.consumerToken(consumerId), offerId);
        assertThat(result.status()).as(result.body()).isEqualTo(HTTP_CREATED);
        state.rememberSavedOffer(http.json(result).path("savedOfferId").asLong());
    }

    @When("consumer {long} saves offer {long}")
    public void consumerSavesOffer(Long consumerId, Long offerId) {
        save(TestIdentity.consumerToken(consumerId), offerId);
    }

    @When("consumer {long} sends a saved offer without offerId")
    public void consumerSavesWithoutOffer(Long consumerId) {
        http.post(SAVED_OFFERS, TestIdentity.consumerToken(consumerId), EMPTY_BODY);
    }

    @When("offer {long} is saved {}")
    public void offerIsSavedWithToken(Long offerId, String tokenKind) {
        save(tokenOfKind(tokenKind), offerId);
    }

    @When("consumer {long} lists the saved offers")
    public void consumerListsSavedOffers(Long consumerId) {
        http.get(SAVED_OFFERS, TestIdentity.consumerToken(consumerId));
    }

    @When("consumer {long} removes offer {long} from the saved offers")
    public void consumerRemovesOffer(Long consumerId, Long offerId) {
        http.delete(SAVED_OFFER.formatted(offerId), TestIdentity.consumerToken(consumerId));
    }

    @And("the response is the same saved offer as before")
    public void responseIsTheSameSavedOffer() throws JsonProcessingException {
        assertThat(http.json(http.last()).path("savedOfferId").asLong()).isEqualTo(state.savedOfferId());
    }

    @And("the list only has offer(s) {longs}")
    public void listOnlyHasOffers(List<Long> offerIds) throws JsonProcessingException {
        var listed = new ArrayList<Long>();
        for (var item : http.json(http.last())) {
            listed.add(item.path("offerId").asLong());
        }
        assertThat(listed).containsExactlyElementsOf(offerIds);
    }

    @And("saved offer {long} has {string} set to {word}")
    public void savedOfferHasField(Long offerId, String field, String value) throws JsonProcessingException {
        var found = false;
        for (var item : http.json(http.last())) {
            if (item.path("offerId").asLong() == offerId) {
                assertThat(item.path(field).asText()).isEqualTo(value);
                found = true;
            }
        }
        assertThat(found).as("offer %d in the list", offerId).isTrue();
    }

    @And("consumer {long} has {int} saved offer(s)")
    public void consumerHasSavedOffers(Long consumerId, int expected) {
        assertThat(savedOffers.findByConsumerIdOrderBySavedAtDesc(consumerId)).hasSize(expected);
    }

    private HttpScenario.HttpResult save(String token, Long offerId) {
        return http.post(SAVED_OFFERS, token, OFFER_BODY.formatted(offerId));
    }

    static String tokenOfKind(String tokenKind) {
        return switch (tokenKind) {
            case "without a token" -> null;
            case "with a business owner token" -> TestIdentity.businessOwnerToken(BUSINESS_OWNER_USER_ID, BUSINESS_ID);
            case "with a token without roles" -> TestIdentity.tokenWithoutRoles(CONSUMER_ID);
            case "with a token from another issuer" -> TestIdentity.tokenFromAnotherIssuer(CONSUMER_ID);
            case "with a token for another audience" -> TestIdentity.tokenForAnotherAudience(CONSUMER_ID);
            case "with a token signed with another key" -> TestIdentity.tokenSignedWithAnotherKey(CONSUMER_ID);
            default -> throw new IllegalArgumentException(tokenKind);
        };
    }
}
