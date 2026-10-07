package com.geopslabs.geops.catalog.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
@ScenarioScope
public class HttpScenario {
    private static final String LOCAL_URL = "http://localhost:";
    private static final String BEARER = "Bearer ";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private HttpResult last;

    public HttpScenario(@Value("${local.server.port}") int port, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(LOCAL_URL + port)
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
        this.objectMapper = objectMapper;
    }

    public HttpResult get(String path, String token) {
        var request = restClient.get().uri(path);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, BEARER + token);
        }
        last = request.exchange((ignored, response) -> new HttpResult(response.getStatusCode().value(),
                StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
        return last;
    }

    public HttpResult last() {
        return last;
    }

    public JsonNode json(HttpResult result) throws JsonProcessingException {
        return objectMapper.readTree(result.body());
    }

    public record HttpResult(int status, String body) {
    }
}
