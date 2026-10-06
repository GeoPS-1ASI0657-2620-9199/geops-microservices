package com.geopslabs.geops.identity.acceptance;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestKeys {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final int PEM_LINE_LENGTH = 64;
    private static final String LINE_BREAK = "\n";
    private static final String PEM_FORMAT = "-----BEGIN %1$s-----%3$s%2$s%3$s-----END %1$s-----%3$s";
    private static final String PRIVATE_LABEL = "PRIVATE KEY";
    private static final String PUBLIC_LABEL = "PUBLIC KEY";
    private static final String DIRECTORY_PREFIX = "identity-test-keys";
    private static final Path PRIVATE_KEY_PATH;
    private static final Path PUBLIC_KEY_PATH;

    static {
        var keyPair = generateKeyPair();
        var directory = createDirectory();
        PRIVATE_KEY_PATH = writePem(directory.resolve("private.pem"), PRIVATE_LABEL,
                keyPair.getPrivate().getEncoded());
        PUBLIC_KEY_PATH = writePem(directory.resolve("public.pem"), PUBLIC_LABEL,
                keyPair.getPublic().getEncoded());
    }

    private TestKeys() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("geops.security.jwt.private-key-path", PRIVATE_KEY_PATH::toString);
        registry.add("geops.security.jwt.public-key-path", PUBLIC_KEY_PATH::toString);
    }

    public static String privateKeyPath() {
        return PRIVATE_KEY_PATH.toString();
    }

    public static String publicKeyPath() {
        return PUBLIC_KEY_PATH.toString();
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Path createDirectory() {
        try {
            var directory = Files.createTempDirectory(DIRECTORY_PREFIX);
            directory.toFile().deleteOnExit();
            return directory;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path writePem(Path path, String label, byte[] der) {
        var encoder = Base64.getMimeEncoder(PEM_LINE_LENGTH, LINE_BREAK.getBytes(StandardCharsets.US_ASCII));
        var pem = PEM_FORMAT.formatted(label, encoder.encodeToString(der), LINE_BREAK);
        try {
            Files.writeString(path, pem, StandardCharsets.US_ASCII);
            path.toFile().deleteOnExit();
            return path;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
