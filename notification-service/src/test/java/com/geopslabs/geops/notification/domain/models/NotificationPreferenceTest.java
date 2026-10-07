package com.geopslabs.geops.notification.domain.models;

import com.geopslabs.geops.notification.domain.models.exceptions.InvalidDailyLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPreferenceTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-08T13:05:00");
    private static final int DAILY_LIMIT = 3;
    private static final int BELOW_LIMIT = 2;

    @Test
    void aChannelThatIsOffDoesNotAllowTheNotice() {
        var preference = new NotificationPreference(1L, false, true, DAILY_LIMIT, NOW);

        assertThat(preference.allows(Channel.WEB_PUSH, 0)).isFalse();
    }

    @Test
    void aChannelThatIsOnAllowsTheNoticeBelowTheLimit() {
        var preference = new NotificationPreference(1L, false, true, DAILY_LIMIT, NOW);

        assertThat(preference.allows(Channel.EMAIL, BELOW_LIMIT)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Channel.class)
    void reachingTheLimitBlocksEveryChannel(Channel channel) {
        var preference = new NotificationPreference(1L, true, true, DAILY_LIMIT, NOW);

        assertThat(preference.allows(channel, DAILY_LIMIT)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {NotificationPreference.MIN_DAILY_LIMIT - 1, NotificationPreference.MAX_DAILY_LIMIT + 1})
    void aDailyLimitOutsideTheRangeIsRejected(int dailyLimit) {
        assertThatThrownBy(() -> new NotificationPreference(1L, true, true, dailyLimit, NOW))
                .isInstanceOf(InvalidDailyLimitException.class);
    }

    @Test
    void theInitialPreferenceHasBothChannelsOff() {
        var preference = NotificationPreference.initialFor(1L, NOW);

        assertThat(preference.allows(Channel.WEB_PUSH, 0)).isFalse();
        assertThat(preference.allows(Channel.EMAIL, 0)).isFalse();
        assertThat(preference.getDailyLimit()).isEqualTo(NotificationPreference.INITIAL_DAILY_LIMIT);
    }
}
