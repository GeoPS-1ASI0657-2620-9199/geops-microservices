package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
@ScenarioScope
public class HttpScenario {
    private static final String LOCAL_URL = "http://localhost:";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private int lastStatus;
    private String lastBody;

    public HttpScenario(@Value("${local.server.port}") int port, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(LOCAL_URL + port)
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
        this.objectMapper = objectMapper;
    }

    public void post(String path, Object body) {
        restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((request, response) -> remember(response.getStatusCode().value(),
                        StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
    }

    public void get(String path) {
        restClient.get().uri(path)
                .exchange((request, response) -> remember(response.getStatusCode().value(),
                        StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
    }

    public int lastStatus() {
        return lastStatus;
    }

    public String lastBody() {
        return lastBody;
    }

    public String field(String name) throws JsonProcessingException {
        return lastJson().path(name).asText();
    }

    public JsonNode lastJson() throws JsonProcessingException {
        return objectMapper.readTree(lastBody);
    }

    private boolean remember(int status, String body) {
        this.lastStatus = status;
        this.lastBody = body;
        return true;
    }
}
