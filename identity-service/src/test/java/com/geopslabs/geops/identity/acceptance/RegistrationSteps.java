package com.geopslabs.geops.identity.acceptance;

import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RegistrationSteps {
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String EXISTING_PHONE = "987123456";
    private static final String EXISTING_PASSWORD = "Ofertas#2026";

    private final HttpScenario http;
    private final UserRepositoryPort userRepository;
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final PasswordHasherPort passwordHasher;

    public RegistrationSteps(HttpScenario http, UserRepositoryPort userRepository,
                             ConsumerProfileRepositoryPort consumerProfileRepository,
                             PasswordHasherPort passwordHasher) {
        this.http = http;
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Dado("que no existe una cuenta con el correo {string}")
    public void noAccountExistsWithEmail(String email) {
        assertThat(userRepository.existsByEmail(new Email(email).value())).isFalse();
    }

    @Dado("que existe una cuenta de {string} con el correo {string}")
    public void anAccountExists(String fullName, String email) {
        userRepository.save(User.register(fullName, new Email(email), EXISTING_PHONE,
                passwordHasher.encode(EXISTING_PASSWORD), Role.CONSUMER));
    }

    @Cuando("se registra como {string} con nombre {string}, correo {string}, teléfono {string} y contraseña {string}")
    public void registers(String role, String fullName, String email, String phone, String password) {
        http.post(REGISTER_PATH, Map.of("role", role, "fullName", fullName, "email", email,
                "phone", phone, "password", password));
    }

    @Y("la cuenta {string} tiene perfil de consumidor")
    public void theAccountHasConsumerProfile(String email) {
        var user = userRepository.findByEmail(new Email(email).value()).orElseThrow();
        assertThat(consumerProfileRepository.existsByUserId(user.getId())).isTrue();
    }
}
