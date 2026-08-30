package com.ajaia.docs.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Slows down password guessing.
 *
 * Without this, the login endpoint will answer as fast as the network allows,
 * which is all an attacker needs to work through a password list. After a run of
 * failures the account is locked for a short window. A correct password clears
 * the count.
 *
 * The counters live in memory, which is the honest limit of this approach: it
 * protects a single instance and resets on restart. Anything running more than
 * one instance needs this in Redis or in front of the app at the gateway. That
 * tradeoff is called out in the architecture note.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(AtomicInteger count, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempts attempts = attemptsByKey.get(key(email));
        if (attempts == null || attempts.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(attempts.lockedUntil())) {
            // The window has passed, so let them try again from a clean count.
            attemptsByKey.remove(key(email));
            return false;
        }
        return true;
    }

    public void recordFailure(String email) {
        attemptsByKey.compute(key(email), (ignored, existing) -> {
            if (existing == null) {
                return new Attempts(new AtomicInteger(1), null);
            }
            int failures = existing.count().incrementAndGet();
            if (failures >= MAX_ATTEMPTS) {
                return new Attempts(existing.count(), Instant.now().plus(LOCK_DURATION));
            }
            return existing;
        });
    }

    public void recordSuccess(String email) {
        attemptsByKey.remove(key(email));
    }

    public Duration remainingLock(String email) {
        Attempts attempts = attemptsByKey.get(key(email));
        if (attempts == null || attempts.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration left = Duration.between(Instant.now(), attempts.lockedUntil());
        return left.isNegative() ? Duration.ZERO : left;
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
