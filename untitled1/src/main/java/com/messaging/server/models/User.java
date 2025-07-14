package com.messaging.server.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class User {
    private final String phoneNumber;
    private final String password;
}
