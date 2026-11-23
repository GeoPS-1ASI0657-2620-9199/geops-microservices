package com.geopslabs.geops.notification.domain.ports;

import java.time.Instant;

public interface ConfirmationEmailRepositoryPort {
    /** Records that the confirmation email of the user is sent; false when it was already recorded. */
    boolean claim(Long userId, Instant sentAt);
}
