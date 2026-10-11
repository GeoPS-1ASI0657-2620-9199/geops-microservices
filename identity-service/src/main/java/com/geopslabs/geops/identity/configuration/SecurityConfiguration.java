package com.geopslabs.geops.identity.configuration;

import com.geopslabs.geops.identity.infrastructure.tokens.RsaKeyProvider;
import com.geopslabs.geops.identity.shared.SecurityErrorHandlers;
import com.nimbusds.jose.JOSEException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.Collection;

/**
 * Identity issues the tokens and also validates them on its own profile routes, with its own public key (no JWKS
 * request over the network). Registration, login and the JWKS stay public and ignore any token sent to them, so a
 * stale token kept by the browser never blocks a new login.
 */
@Configuration
public class SecurityConfiguration {
    private static final String ROLE_CONSUMER = "ROLE_CONSUMER";
    private static final String ROLE_BUSINESS_OWNER = "ROLE_BUSINESS_OWNER";
    private static final String ROLES_CLAIM = "roles";
    private static final String NO_PREFIX = "";
    private static final String BUSINESS_PROFILES = "/api/v1/business-profiles/**";
    private static final String CONSUMER_PROFILES = "/api/v1/consumer-profiles/**";
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**", "/.well-known/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/actuator/health", "/actuator/health/**", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandlers errorHandlers)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SecurityConfiguration::authorizeRequests)
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(RsaKeyProvider keyProvider, JwtProperties properties) throws JOSEException {
        var decoder = NimbusJwtDecoder.withPublicKey(keyProvider.signingKey().toRSAPublicKey()).build();
        decoder.setJwtValidator(validatorFor(properties));
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> validatorFor(JwtProperties properties) {
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                audiences -> audiences != null && audiences.contains(properties.audience()));
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), audience);
    }

    private static void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests) {
        requests
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(BUSINESS_PROFILES).hasAuthority(ROLE_BUSINESS_OWNER)
                .requestMatchers(CONSUMER_PROFILES).hasAuthority(ROLE_CONSUMER)
                .anyRequest().denyAll();
    }

    private static BearerTokenResolver bearerTokenResolver() {
        RequestMatcher publicPaths = new OrRequestMatcher(Arrays.stream(PUBLIC_PATHS)
                .map(path -> (RequestMatcher) AntPathRequestMatcher.antMatcher(path))
                .toList());
        var resolver = new DefaultBearerTokenResolver();
        return request -> publicPaths.matches(request) ? null : resolver.resolve(request);
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
