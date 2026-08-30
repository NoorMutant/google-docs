package com.ajaia.docs;

import com.ajaia.docs.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final String EMAIL = "target@test.local";

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void aFreshAccountIsNotLocked() {
        assertThat(service.isLocked(EMAIL)).isFalse();
    }

    @Test
    void staysUnlockedUntilTheLimitIsReached() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure(EMAIL);
            assertThat(service.isLocked(EMAIL)).isFalse();
        }
    }

    @Test
    void locksOnTheFifthFailure() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(EMAIL);
        }

        assertThat(service.isLocked(EMAIL)).isTrue();
        assertThat(service.remainingLock(EMAIL)).isPositive();
    }

    @Test
    void aSuccessfulSignInClearsTheCount() {
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);
        service.recordSuccess(EMAIL);

        for (int i = 0; i < 4; i++) {
            service.recordFailure(EMAIL);
        }

        // The counter restarted, so four more failures is still under the limit.
        assertThat(service.isLocked(EMAIL)).isFalse();
    }

    @Test
    void lockingOneAccountDoesNotAffectAnother() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(EMAIL);
        }

        assertThat(service.isLocked("someone.else@test.local")).isFalse();
    }

    @Test
    void theEmailIsMatchedWithoutRegardToCaseOrSurroundingSpace() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("Target@Test.Local");
        }

        assertThat(service.isLocked("  target@test.local  ")).isTrue();
    }

    @Test
    void anUnknownAccountReportsNoRemainingLock() {
        assertThat(service.remainingLock("nobody@test.local")).isZero();
    }
}
