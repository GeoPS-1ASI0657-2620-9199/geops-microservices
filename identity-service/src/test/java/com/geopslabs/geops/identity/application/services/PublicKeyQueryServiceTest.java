package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.acceptance.TestKeys;
import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.geopslabs.geops.identity.infrastructure.tokens.RsaKeyProvider;
import com.geopslabs.geops.identity.infrastructure.tokens.RsaTokenIssuerAdapter;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicKeyQueryServiceTest {
    private static final String ISSUER = "geops-identity";
    private static final String AUDIENCE = "geops-api";
    private static final String KEYS = "keys";
    private static final Set<String> PUBLIC_MEMBERS = Set.of("kty", "use", "alg", "kid", "n", "e");

    private final JwtProperties properties =
            new JwtProperties(TestKeys.privateKeyPath(), TestKeys.publicKeyPath(), ISSUER, AUDIENCE);
    private final PublicKeyQueryService service = new PublicKeyQueryService(
            new RsaTokenIssuerAdapter(new RsaKeyProvider(properties), properties, Clock.systemUTC()));

    @Test
    void publishesOneRs256SigningKeyWithKid() throws ParseException {
        var key = JWKSet.parse(service.publicKeys()).getKeys().get(0);

        assertThat(key.getKeyType().getValue()).isEqualTo("RSA");
        assertThat(key.getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(key.getKeyUse().identifier()).isEqualTo("sig");
        assertThat(key.getKeyID()).isNotBlank();
    }

    @Test
    void exposesOnlyThePublicKey() throws ParseException {
        var jwkSet = JWKSet.parse(service.publicKeys());

        assertThat(jwkSet.getKeys()).hasSize(1).allSatisfy(key -> assertThat(key.isPrivate()).isFalse());
        var members = (Map<?, ?>) ((List<?>) service.publicKeys().get(KEYS)).get(0);
        assertThat(members.keySet()).isEqualTo(PUBLIC_MEMBERS);
    }
}
