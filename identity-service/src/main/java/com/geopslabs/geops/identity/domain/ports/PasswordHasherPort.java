package com.geopslabs.geops.identity.domain.ports;

public interface PasswordHasherPort {
    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
