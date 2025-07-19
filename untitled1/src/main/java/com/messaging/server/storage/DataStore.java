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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.messaging.server.models.Host;
import com.messaging.server.models.HostConnection;
import com.messaging.server.models.Token;
import com.messaging.server.models.User;
import com.messaging.server.models.Workspace;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

@Slf4j
public class DataStore {
    private static final DataStore INSTANCE = new DataStore();
    private static class PersistentState {
        ConcurrentHashMap<String, User> users;
        ConcurrentHashMap<String, Host> hosts;
        ConcurrentHashMap<String, Workspace> workspaces;
    }
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
    public synchronized void saveStateToFile(String filePath) {
        log.info("Attempting to save server state to {}...", filePath);
        PersistentState state = new PersistentState();
        state.users = this.users;
        state.hosts = this.hosts;
        state.workspaces = this.workspaces;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(state, writer);
            log.info("Server state successfully saved.");
        } catch (IOException e) {
            log.error("Failed to save server state to file.", e);
        }
    }
    public synchronized void loadStateFromFile(String filePath) {
        log.info("Attempting to load server state from {}...", filePath);
        Gson gson = new Gson();
        try (Reader reader = new FileReader(filePath)) {

            PersistentState loadedState = gson.fromJson(reader, PersistentState.class);

            if (loadedState != null) {
                // --- تغییر کلیدی برای حل مشکل final ---
                // به جای re-assign کردن، مپ فعلی را پاک کرده و داده‌های جدید را اضافه می‌کنیم.
                if (loadedState.users != null) {
                    this.users.clear();
                    this.users.putAll(loadedState.users);
                }
                if (loadedState.hosts != null) {
                    this.hosts.clear();
                    this.hosts.putAll(loadedState.hosts);
                }
                if (loadedState.workspaces != null) {
                    this.workspaces.clear();
                    this.workspaces.putAll(loadedState.workspaces);
                }

                log.info("Server state successfully loaded. Found {} users, {} hosts, {} workspaces.",
                        this.users.size(), this.hosts.size(), this.workspaces.size());
            } else {
                log.warn("Data file was empty or corrupted. Starting with a fresh state.");
            }

        } catch (IOException e) {
            log.warn("No existing data file found at '{}'. Starting with a fresh state.", filePath);
        } catch (Exception e) {
            log.error("Failed to load or parse server state from file. Starting with a fresh state.", e);
        }
    }

}
