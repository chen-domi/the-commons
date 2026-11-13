package com.thecommons.backend.nlp;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Single-instance safeguards, not a billing cap or a distributed rate limiter. */
@Component
public class NlpRateLimiter {
    private final Clock clock;
    private final Map<String, Integer> userCounts = new HashMap<>();
    private long window = -1;
    private int total;
    private boolean busy;

    public NlpRateLimiter() { this(Clock.systemUTC()); }
    NlpRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void acquire(String subject) {
        long currentWindow = clock.millis() / 60_000;
        if (currentWindow != window) {
            window = currentWindow;
            userCounts.clear();
            total = 0;
        }
        if (busy || total >= 30 || userCounts.getOrDefault(subject, 0) >= 5) {
            throw new NlpException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI is busy or the request limit was reached. Please wait a minute or use manual entry.");
        }
        busy = true;
        total++;
        userCounts.merge(subject, 1, Integer::sum);
    }

    public synchronized void release() { busy = false; }
}
