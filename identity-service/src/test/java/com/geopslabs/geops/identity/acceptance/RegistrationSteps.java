package com.geopslabs.geops.identity.acceptance;

import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RegistrationSteps {
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String EXISTING_PHONE = "987123456";
    private static final String EXISTING_PASSWORD = "Ofertas#2026";
    private static final String DECIMAL = "(-?\\d+(?:\\.\\d+)?)";
    private static final String OWNER_NAME = "Rosa Quispe Mamani";
    private static final String OWNER_EMAIL = "rosa.quispe@ejemplo.pe";
    private static final String OWNER_PHONE = "987654321";
    private static final String OWNER_PASSWORD = "Bodega#2026";
    private static final String BUSINESS_TYPE = "Bodega";
    private static final String OPENING_HOURS = "Lun-Sáb 07:00-22:00";

    private final HttpScenario http;
    private final UserRepositoryPort userRepository;
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final BusinessProfileRepositoryPort businessProfileRepository;
    private final PasswordHasherPort passwordHasher;

    public RegistrationSteps(HttpScenario http, UserRepositoryPort userRepository,
                             ConsumerProfileRepositoryPort consumerProfileRepository,
                             BusinessProfileRepositoryPort businessProfileRepository,
                             PasswordHasherPort passwordHasher) {
        this.http = http;
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.businessProfileRepository = businessProfileRepository;
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

    @Cuando("^registra el negocio \"([^\"]*)\" con RUC \"([^\"]*)\", dirección \"([^\"]*)\", "
            + "latitud " + DECIMAL + " y longitud " + DECIMAL + "$")
    public void registersBusiness(String businessName, String ruc, String address, String latitude,
                                  String longitude) {
        var businessProfile = new LinkedHashMap<String, Object>();
        businessProfile.put("businessName", businessName);
        businessProfile.put("businessType", BUSINESS_TYPE);
        businessProfile.put("ruc", ruc);
        businessProfile.put("address", address);
        businessProfile.put("latitude", Double.valueOf(latitude));
        businessProfile.put("longitude", Double.valueOf(longitude));
        businessProfile.put("openingHours", OPENING_HOURS);
        http.post(REGISTER_PATH, Map.of("role", "BUSINESS_OWNER", "fullName", OWNER_NAME, "email", OWNER_EMAIL,
                "phone", OWNER_PHONE, "password", OWNER_PASSWORD, "businessProfile", businessProfile));
    }

    @Y("la cuenta {string} tiene perfil de negocio")
    public void theAccountHasBusinessProfile(String email) {
        var user = userRepository.findByEmail(new Email(email).value()).orElseThrow();
        assertThat(businessProfileRepository.existsByUserId(user.getId())).isTrue();
    }
}
