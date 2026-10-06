package com.geopslabs.geops.identity.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;

import java.text.ParseException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationSteps {
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String CONSUMER_NAME = "Lucía Fernández Ríos";
    private static final String CONSUMER_PHONE = "987123456";

    private final HttpScenario http;
    private final UserRepositoryPort userRepository;
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final PasswordHasherPort passwordHasher;

    public AuthenticationSteps(HttpScenario http, UserRepositoryPort userRepository,
                               ConsumerProfileRepositoryPort consumerProfileRepository,
                               PasswordHasherPort passwordHasher) {
        this.http = http;
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Dado("que existe el consumidor {string} con contraseña {string}")
    public void aConsumerExists(String email, String password) {
        var user = userRepository.save(User.register(CONSUMER_NAME, new Email(email), CONSUMER_PHONE,
                passwordHasher.encode(password), Role.CONSUMER));
        consumerProfileRepository.save(ConsumerProfile.createFor(user.getId()));
    }

    @Cuando("inicia sesión con {string} y {string}")
    public void logsIn(String email, String password) {
        http.post(LOGIN_PATH, Map.of("email", email, "password", password));
    }

    @Y("la firma del token se valida con la clave publicada en {string}")
    public void theTokenSignatureIsValidatedWithThePublishedKey(String jwksPath)
            throws JsonProcessingException, ParseException, JOSEException {
        var token = SignedJWT.parse(http.field(ACCESS_TOKEN));
        http.get(jwksPath);
        var publishedKey = JWKSet.parse(http.lastBody()).getKeyByKeyId(token.getHeader().getKeyID());

        assertThat(token.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(publishedKey).isNotNull();
        assertThat(token.verify(new RSASSAVerifier(publishedKey.toRSAKey()))).isTrue();
    }
}
