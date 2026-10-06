package com.geopslabs.geops.engagement.acceptance;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TestIdentity {
    public static final String ISSUER = "geops-identity";
    public static final String AUDIENCE = "geops-api";
    private static final String OTHER_ISSUER = "someone-else";
    private static final String OTHER_AUDIENCE = "other-api";
    private static final String ROLES_CLAIM = "roles";
    private static final String BUSINESS_ID_CLAIM = "businessId";
    private static final String KEY_ID = "acceptance-key";
    private static final String OTHER_KEY_ID = "unknown-key";
    private static final int KEY_SIZE = 2048;
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    private static final String LOOPBACK = "127.0.0.1";
    private static final int ANY_FREE_PORT = 0;
    private static final int NO_BACKLOG = 0;
    private static final int HTTP_OK = 200;
    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final RSAKey SIGNING_KEY = generateKey(KEY_ID);
    private static final RSAKey FOREIGN_KEY = generateKey(OTHER_KEY_ID);
    private static final HttpServer JWKS_SERVER = startJwksServer();

    private TestIdentity() {
    }

    public static String jwksUri() {
        return "http://" + LOOPBACK + ":" + JWKS_SERVER.getAddress().getPort() + JWKS_PATH;
    }

    public static String consumerToken(Long userId) {
        return sign(SIGNING_KEY, claims(userId, ISSUER, AUDIENCE, List.of("ROLE_CONSUMER")).build());
    }

    public static String businessOwnerToken(Long userId, Long businessId) {
        var claims = claims(userId, ISSUER, AUDIENCE, List.of("ROLE_BUSINESS_OWNER"))
                .claim(BUSINESS_ID_CLAIM, businessId);
        return sign(SIGNING_KEY, claims.build());
    }

    public static String tokenWithoutRoles(Long userId) {
        return sign(SIGNING_KEY, claims(userId, ISSUER, AUDIENCE, List.of()).build());
    }

    public static String tokenFromAnotherIssuer(Long userId) {
        return sign(SIGNING_KEY, claims(userId, OTHER_ISSUER, AUDIENCE, List.of("ROLE_CONSUMER")).build());
    }

    public static String tokenForAnotherAudience(Long userId) {
        return sign(SIGNING_KEY, claims(userId, ISSUER, OTHER_AUDIENCE, List.of("ROLE_CONSUMER")).build());
    }

    public static String tokenSignedWithAnotherKey(Long userId) {
        return sign(FOREIGN_KEY, claims(userId, ISSUER, AUDIENCE, List.of("ROLE_CONSUMER")).build());
    }

    private static JWTClaimsSet.Builder claims(Long userId, String issuer, String audience, List<String> roles) {
        var issuedAt = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .audience(audience)
                .claim(ROLES_CLAIM, roles)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(TOKEN_LIFETIME)));
    }

    private static String sign(RSAKey key, JWTClaimsSet claims) {
        try {
            var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build();
            var token = new SignedJWT(header, claims);
            token.sign(new RSASSASigner(key));
            return token.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(KEY_SIZE).keyID(keyId).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress(LOOPBACK, ANY_FREE_PORT), NO_BACKLOG);
            var jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext(JWKS_PATH, exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(HTTP_OK, jwks.length);
                exchange.getResponseBody().write(jwks);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
