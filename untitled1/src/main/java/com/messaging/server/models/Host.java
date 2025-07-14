package com.messaging.server.models;

import lombok.Getter;
import lombok.ToString;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@ToString
public class Host {
    private final String id;
    private final String address;
    private final int startPort;
    private final int endPort;
    private final Set<Integer> usedPorts = new HashSet<>();

    public Host(String address, int startPort, int endPort) {
        this.address = address;
        this.startPort = startPort;
        this.endPort = endPort;
        this.id = address + ":" + startPort;
    }

    public synchronized int getAvailablePort() {
        if (usedPorts.size() > (endPort - startPort)) {
            return -1;
        }
        while (true) {
            int randomPort = ThreadLocalRandom.current().nextInt(startPort, endPort + 1);
            if (!usedPorts.contains(randomPort)) {
                usedPorts.add(randomPort);
                return randomPort;
            }
        }
    }
}
