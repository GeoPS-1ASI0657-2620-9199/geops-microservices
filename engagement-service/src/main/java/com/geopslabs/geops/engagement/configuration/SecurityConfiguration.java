package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.shared.web.SecurityErrorHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    private static final String ROLE_CONSUMER = "ROLE_CONSUMER";
    private static final String ROLES_CLAIM = "roles";
    private static final String NO_PREFIX = "";
    private static final String SAVED_OFFERS = "/api/v1/saved-offers";
    private static final String SAVED_OFFER_PATHS = "/api/v1/saved-offers/**";
    private static final String REVIEWS = "/api/v1/reviews";
    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/actuator/health/**", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandlers errorHandlers)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfiguration::authorizeRequests)
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
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests) {
        requests
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.POST, SAVED_OFFERS).hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.GET, SAVED_OFFERS).hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.DELETE, SAVED_OFFER_PATHS).hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.POST, REVIEWS).hasAuthority(ROLE_CONSUMER)
                .requestMatchers(HttpMethod.GET, REVIEWS).authenticated()
                .anyRequest().denyAll();
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
