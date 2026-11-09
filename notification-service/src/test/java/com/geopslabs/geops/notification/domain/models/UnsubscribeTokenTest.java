package com.geopslabs.geops.notification.domain.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.geopslabs.geops.notification.domain.models.exceptions.InvalidUnsubscribeTokenException;

class UnsubscribeTokenTest {
    private static final String SECRET = "test-only-signing-key";
    @Test
    void validTokenRoundTripsAndFitsTheSchema() {
        var token = UnsubscribeToken.issue(2001L, NotificationType.NEARBY_OFFER, SECRET);
        assertThat(token.length()).isLessThanOrEqualTo(64);
        assertThat(UnsubscribeToken.verify(token, SECRET)).isEqualTo(new UnsubscribeToken(2001L, NotificationType.NEARBY_OFFER));
    }
    @Test
    void tamperingWithConsumerTypeOrSignatureIsRejected() {
        var token = UnsubscribeToken.issue(2001L, NotificationType.NEARBY_OFFER, SECRET);
        for (var invalid : new String[]{token.replace("2001", "2002"), token.replace("NEARBY_OFFER", "FOLLOWED_BUSINESS_OFFER"), token + "x", "bad", "0.NEARBY_OFFER.bad"}) {
            assertThatThrownBy(() -> UnsubscribeToken.verify(invalid, SECRET)).isInstanceOf(InvalidUnsubscribeTokenException.class);
        }
    }
    @Test
    void reportMailCannotBeUnsubscribedAndAnotherKeyCannotVerify() {
        assertThatThrownBy(() -> UnsubscribeToken.issue(2001L, NotificationType.REPORT_RECEIVED, SECRET)).isInstanceOf(InvalidUnsubscribeTokenException.class);
        var token = UnsubscribeToken.issue(2001L, NotificationType.NEARBY_OFFER, SECRET);
        assertThatThrownBy(() -> UnsubscribeToken.verify(token, "another-key")).isInstanceOf(InvalidUnsubscribeTokenException.class);
    }
}
