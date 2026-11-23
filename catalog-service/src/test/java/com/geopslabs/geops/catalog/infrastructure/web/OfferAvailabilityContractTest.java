package com.geopslabs.geops.catalog.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.catalog.application.usecases.GetOfferAvailabilityUseCase;
import com.geopslabs.geops.catalog.application.usecases.OfferAvailability;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Provider side of the contract Reservation -> Catalog (US15). Every interaction of
 * contracts/catalog-offer-availability.v1.json, the file reservation-service stubs Catalog with, is replayed against
 * the real controller and error handler; the status and the whole body must be the ones of the contract, with no
 * field more or less. The use case is put in the state each interaction describes; whether an offer is available
 * is decided by OfferQueryService and checked by offer-availability.feature. The JSON converter is configured as
 * Spring Boot configures it (dates as ISO text).
 */
class OfferAvailabilityContractTest {
    private static final String CONTRACT = "/contracts/catalog-offer-availability.v1.json";
    private static final Pattern OFFER_PATH = Pattern.compile("/internal/v1/offers/(\\d+)/availability");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> catalogAnswersEveryInteractionReservationExpects() {
        return interactions().map(interaction -> DynamicTest.dynamicTest(
                interaction.path("description").asText(), () -> verify(interaction)));
    }

    private void verify(JsonNode interaction) throws Exception {
        var request = interaction.path("request");
        var expected = interaction.path("response");
        var path = OFFER_PATH.matcher(request.path("path").asText());
        assertThat(path.matches()).as("path of %s", interaction.path("description").asText()).isTrue();
        var offerId = Long.valueOf(path.group(1));

        var useCase = mock(GetOfferAvailabilityUseCase.class);
        if (expected.path("status").asInt() == 404) {
            when(useCase.getAvailability(new GetOfferByIdQuery(offerId))).thenThrow(new OfferNotFoundException(offerId));
        } else {
            var body = expected.path("body");
            when(useCase.getAvailability(new GetOfferByIdQuery(offerId))).thenReturn(new OfferAvailability(offerId,
                    body.path("businessId").isNull() ? null : body.path("businessId").asLong(),
                    body.path("title").asText(), LocalDate.parse(body.path("validTo").asText()),
                    body.path("available").asBoolean()));
        }

        var response = mockMvc(useCase)
                .perform(request(org.springframework.http.HttpMethod.valueOf(request.path("method").asText()),
                        request.path("path").asText()).accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(expected.path("status").asInt());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(MAPPER.readTree(response.getContentAsByteArray())).isEqualTo(expected.path("body"));
    }

    private static MockMvc mockMvc(GetOfferAvailabilityUseCase useCase) {
        var json = new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build());
        return MockMvcBuilders.standaloneSetup(new InternalOffersController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(json)
                .build();
    }

    private static Stream<JsonNode> interactions() {
        try (var in = OfferAvailabilityContractTest.class.getResourceAsStream(CONTRACT)) {
            assertThat(in).as("src/test/resources%s", CONTRACT).isNotNull();
            var contract = MAPPER.readTree(in);
            assertThat(contract.path("provider").asText()).isEqualTo("catalog-service");
            return StreamSupport.stream(contract.path("interactions").spliterator(), false).toList().stream();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
