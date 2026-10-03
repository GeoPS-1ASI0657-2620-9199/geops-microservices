package com.geopslabs.geops.identity.infrastructure.hashing;

import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This interface is a marker interface for the BCrypt hashing service.
 * It extends the {@link PasswordHasherPort} and {@link PasswordEncoder} interfaces.
 */
public interface BCryptHashingService extends PasswordHasherPort, PasswordEncoder {
}
