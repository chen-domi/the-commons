package com.thecommons.backend.nlp;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class NlpRateLimiterTest {
    @Test void preventsConcurrentRequestsAndAllowsRelease() {
        var limiter = new NlpRateLimiter();
        limiter.acquire("a");
        assertEquals(429, assertThrows(NlpException.class, () -> limiter.acquire("b")).status().value());
        limiter.release();
        assertDoesNotThrow(() -> limiter.acquire("b"));
    }

    @Test void enforcesPerUserQuotaAndResetsNextMinute() {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        var limiter = new NlpRateLimiter(clock);
        for (int i = 0; i < 5; i++) { limiter.acquire("a"); limiter.release(); }
        assertThrows(NlpException.class, () -> limiter.acquire("a"));
        assertDoesNotThrow(() -> limiter.acquire("b"));
        limiter.release();
        when(clock.millis()).thenReturn(60_000L);
        assertDoesNotThrow(() -> limiter.acquire("a"));
    }

    @Test void enforcesGlobalQuotaAcrossUsers() {
        var limiter = new NlpRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 30; i++) { limiter.acquire("user" + i); limiter.release(); }
        assertThrows(NlpException.class, () -> limiter.acquire("new-user"));
    }
}
