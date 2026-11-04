package com.geopslabs.geops.identity.infrastructure.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void keepsTheIdentifierOfTheGatewayDuringTheRequestAndClearsItAfterwards() throws Exception {
        var seen = new AtomicReference<String>();
        var response = new MockHttpServletResponse();

        filter.doFilter(request("req-42.a_b"), response, (FilterChain) (req, res) -> seen.set(MDC.get("correlationId")));

        assertThat(seen.get()).isEqualTo("req-42.a_b");
        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("req-42.a_b");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void createsOneWhenTheRequestHasNoneOrAnUnsafeOne() throws Exception {
        for (var incoming : new String[]{null, "", "a b\nforged line", "x".repeat(101)}) {
            var seen = new AtomicReference<String>();

            filter.doFilter(request(incoming), new MockHttpServletResponse(),
                    (FilterChain) (req, res) -> seen.set(MDC.get("correlationId")));

            assertThat(seen.get()).matches("[0-9a-f-]{36}");
        }
    }

    private static MockHttpServletRequest request(String correlationId) {
        var request = new MockHttpServletRequest("GET", "/api/v1/anything");
        if (correlationId != null) {
            request.addHeader("X-Correlation-ID", correlationId);
        }
        return request;
    }
}
