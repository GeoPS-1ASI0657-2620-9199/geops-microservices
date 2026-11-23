package com.geopslabs.geops.notification.configuration;

import com.geopslabs.geops.notification.shared.web.SecurityErrorHandlers;
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
    private static final String ROLE_CONSUMER = "ROLE_CONSUMER";
    private static final String ROLE_BUSINESS_OWNER = "ROLE_BUSINESS_OWNER";
    private static final String ROLES_CLAIM = "roles";
    private static final String NO_PREFIX = "";
    private static final String NOTIFICATION_PREFERENCES = "/api/v1/notification-preferences";
    private static final String LAST_LOCATION = "/api/v1/locations/last";
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
                // does not route: health, metrics and circuit breakers stay inside the network without a token.
                .requestMatchers(managementPort).permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/v1/push-subscriptions", "/api/v1/push-subscriptions/**").hasAuthority(ROLE_CONSUMER)
                .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**").hasAnyAuthority(ROLE_CONSUMER, ROLE_BUSINESS_OWNER)
                .requestMatchers(HttpMethod.POST, "/api/v1/email-unsubscriptions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/notification-preferences").hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.PUT, NOTIFICATION_PREFERENCES).hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.PUT, LAST_LOCATION).hasAuthority(ROLE_CONSUMER)
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
