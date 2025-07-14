package com.messaging.server.models;

import lombok.Getter;
import java.time.Instant;

@Getter
public class Token {
    private final String value;
    private final String phoneNumber;
    private final Instant creationTime;
    private static final long EXPIRATION_TIME_SECONDS = 300; // 5 minutes

    public Token(String value, String phoneNumber) {
        this.value = value;
        this.phoneNumber = phoneNumber;
        this.creationTime = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(creationTime.plusSeconds(EXPIRATION_TIME_SECONDS));
    }
}

