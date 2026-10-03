package com.geopslabs.geops.identity.infrastructure.tokens;

import com.geopslabs.geops.identity.configuration.JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
public class RsaKeyProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RsaKeyProvider.class);
    private static final String KEY_ALGORITHM = "RSA";
    private static final String PEM_BOUNDARY = "-----[A-Z ]+-----";
    private static final String WHITESPACE = "\\s";
    private static final String EMPTY = "";

    private final RSAKey signingKey;

    public RsaKeyProvider(JwtProperties properties) {
        this.signingKey = buildSigningKey(properties.privateKeyPath(), properties.publicKeyPath());
        LOGGER.info("jwt.keys.loaded kid={}", signingKey.getKeyID());
    }

    public RSAKey signingKey() {
        return signingKey;
    }

    public Map<String, Object> publicJwkSet() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }

    private static RSAKey buildSigningKey(String privateKeyPath, String publicKeyPath) {
        try {
            var keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            var publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(readPem(publicKeyPath)));
            var privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readPem(privateKeyPath)));
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256).keyIDFromThumbprint().build();
        } catch (IOException | GeneralSecurityException | JOSEException exception) {
            throw new IllegalStateException("JWT keys could not be loaded", exception);
        }
    }

    private static byte[] readPem(String path) throws IOException {
        var pem = Files.readString(Path.of(path), StandardCharsets.US_ASCII);
        var base64 = pem.replaceAll(PEM_BOUNDARY, EMPTY).replaceAll(WHITESPACE, EMPTY);
        return Base64.getDecoder().decode(base64);
    }
}
