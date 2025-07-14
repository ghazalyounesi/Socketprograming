package com.messaging.server.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Workspace {
    private final String name;
    private final Host host;
    private final int port;
    private final String creatorPhoneNumber;
}
