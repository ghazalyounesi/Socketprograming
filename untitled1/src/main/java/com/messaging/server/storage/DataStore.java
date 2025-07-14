package com.messaging.server.storage;

import com.messaging.server.models.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
public class DataStore {
    private static final DataStore INSTANCE = new DataStore();

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); // Key: phoneNumber
    private final ConcurrentHashMap<String, Host> hosts = new ConcurrentHashMap<>(); // Key: hostId
    private final ConcurrentHashMap<String, Workspace> workspaces = new ConcurrentHashMap<>(); // Key: workspaceName
    private final ConcurrentHashMap<String, Token> tokens = new ConcurrentHashMap<>(); // Key: tokenValue
    private final ConcurrentHashMap<String, HostConnection> activeHostConnections = new ConcurrentHashMap<>(); // Key: hostId

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // --- User Management ---
    public boolean isUserRegistered(String phoneNumber) { return users.containsKey(phoneNumber); }
    public void registerUser(User user) { users.put(user.getPhoneNumber(), user); }
    public Optional<User> findUser(String phoneNumber) { return Optional.ofNullable(users.get(phoneNumber)); }

    // --- Host Management ---
    public boolean isPortRangeInUse(int startPort, int endPort) {
        return hosts.values().stream().anyMatch(host ->
                (startPort >= host.getStartPort() && startPort <= host.getEndPort()) ||
                        (endPort >= host.getStartPort() && endPort <= host.getEndPort())
        );
    }
    public void registerHost(Host host) { hosts.put(host.getId(), host); }
    public void registerActiveHostConnection(String hostId, HostConnection connection) {
        activeHostConnections.put(hostId, connection);
        log.info("Host connection for {} is now persistent and active.", hostId);
    }
    public Optional<Host> getRandomAvailableHost() {
        if (activeHostConnections.isEmpty()) {
            return Optional.empty();
        }
        List<String> activeHostIds = new ArrayList<>(activeHostConnections.keySet());
        String randomHostId = activeHostIds.get(ThreadLocalRandom.current().nextInt(activeHostIds.size()));
        return Optional.ofNullable(hosts.get(randomHostId));
    }
    public Optional<HostConnection> getActiveHostConnection(String hostId) {
        return Optional.ofNullable(activeHostConnections.get(hostId));
    }
    public void removeHost(String hostId) {
        hosts.remove(hostId);
        HostConnection connection = activeHostConnections.remove(hostId);
        if (connection != null) {
            try {
                connection.getSocket().close();
            } catch (IOException e) {
                log.error("Error closing socket for removed host {}", hostId, e);
            }
        }
        log.warn("Removed host {} from data store and closed its connection.", hostId);
    }

    // --- Workspace Management ---
    public boolean isWorkspaceNameTaken(String name) { return workspaces.containsKey(name); }
    public void createWorkspace(Workspace workspace) { workspaces.put(workspace.getName(), workspace); }
    public Optional<Workspace> findWorkspace(String name) { return Optional.ofNullable(workspaces.get(name)); }

    // --- Token Management ---
    public void saveToken(Token token) { tokens.put(token.getValue(), token); }
    public Optional<Token> findAndInvalidateToken(String tokenValue) {
        Token token = tokens.remove(tokenValue);
        if (token != null && !token.isExpired()) return Optional.of(token);
        return Optional.empty();
    }
}
