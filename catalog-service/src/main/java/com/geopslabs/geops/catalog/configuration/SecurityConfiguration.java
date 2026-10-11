package com.geopslabs.geops.catalog.configuration;

import com.geopslabs.geops.catalog.shared.web.SecurityErrorHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfiguration {
    private static final String ROLE_BUSINESS_OWNER = "ROLE_BUSINESS_OWNER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLES_CLAIM = "roles";
    private static final String NO_PREFIX = "";
    private static final String ANY_OFFER_PATH = "/api/v1/offers/**";
    private static final String CAMPAIGNS = "/api/v1/campaigns";
    private static final String ANY_CAMPAIGN_PATH = "/api/v1/campaigns/**";
    private static final String ANY_ADMIN_PATH = "/api/v1/admin/**";
    private static final String ANY_INTERNAL_PATH = "/internal/v1/**";
    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/actuator/health/**", "/error"
    };
    private static final String LOCAL_MANAGEMENT_PORT = "local.management.port";
    private static final String MANAGEMENT_PORT = "management.server.port";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandlers errorHandlers,
                                                   Environment environment) throws Exception {
        var managementPort = onManagementPort(environment);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> authorizeRequests(requests, managementPort))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .build();
    }

    private static void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests,
            RequestMatcher managementPort) {
        requests
                // Actuator answers only on the management port, which compose does not publish and the gateway
                // does not route: metrics stay inside the network without a token (F2 of the review).
                .requestMatchers(managementPort).permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.GET, ANY_OFFER_PATH).permitAll()
                .requestMatchers(HttpMethod.GET, ANY_INTERNAL_PATH).permitAll()
                .requestMatchers(CAMPAIGNS, ANY_CAMPAIGN_PATH).hasAuthority(ROLE_BUSINESS_OWNER)
                .requestMatchers(ANY_ADMIN_PATH).hasAuthority(ROLE_ADMIN)
                .anyRequest().denyAll();
    }

    /** The port is read on each request: in tests it is random and only known once the server started. */
    private static RequestMatcher onManagementPort(Environment environment) {
        return request -> {
            var port = environment.getProperty(LOCAL_MANAGEMENT_PORT, environment.getProperty(MANAGEMENT_PORT));
            return port != null && Integer.parseInt(port) == request.getLocalPort();
        };
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix(NO_PREFIX);
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
