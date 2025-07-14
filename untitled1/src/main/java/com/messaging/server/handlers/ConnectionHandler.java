package com.messaging.server.handlers;

import com.messaging.server.models.*;
import com.messaging.server.storage.DataStore;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ConnectionHandler implements Runnable {

    private final Socket socket;
    private final DataStore dataStore;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean keepConnectionAlive = false;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        this.dataStore = DataStore.getInstance();
    }

    @Override
    public void run() {
        try {
            setupStreams();
            String commandLine = reader.readLine();
            if (commandLine != null) {
                log.info("Received command from [{}]: {}", socket.getRemoteSocketAddress(), commandLine);
                processInitialCommand(commandLine);
            }
        } catch (IOException e) {
            log.error("IO error with connection [{}]: {}", socket.getRemoteSocketAddress(), e.getMessage());
        } finally {
            if (!keepConnectionAlive) {
                closeConnection();
            } else {
                log.info("Connection for {} will be kept alive.", socket.getRemoteSocketAddress());
            }
        }
    }

    private void setupStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    private void processInitialCommand(String commandLine) throws IOException {
        String[] parts = commandLine.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "register": handleRegister(parts); break;
            case "login": handleLogin(parts); break;
            case "create-host": handleCreateHost(parts); break;
            case "create-workspace": handleCreateWorkspace(parts); break;
            case "connect-workspace": handleConnectWorkspace(parts); break;
            case "whois": handleWhois(parts); break;
            default: writer.println("ERROR Unknown command");
        }
    }

    private void handleCreateHost(String[] parts) throws IOException {
        // ... (validation logic as before) ...
        String address = "127.0.0.1"; // Assuming localhost for simplicity
        int startPort = Integer.parseInt(parts[1].split(" ")[1]);
        int endPort = Integer.parseInt(parts[1].split(" ")[2]);

        // --- Handshake ---
        int randomPort = ThreadLocalRandom.current().nextInt(startPort, endPort + 1);
        writer.println("OK " + randomPort);
        String checkResponse = reader.readLine();
        if (checkResponse == null || !checkResponse.equalsIgnoreCase("check")) {
            writer.println("ERROR Invalid handshake sequence.");
            return;
        }
        String verificationCode = String.format("%010d", new SecureRandom().nextLong(1_000_000_000L, 10_000_000_000L));
        try (Socket verificationSocket = new Socket(address, randomPort)) {
            new PrintWriter(verificationSocket.getOutputStream(), true).println(verificationCode);
        } catch (IOException e) {
            writer.println("ERROR Could not connect to host on verification port.");
            return;
        }
        String codeFromHost = reader.readLine();
        if (verificationCode.equals(codeFromHost)) {
            Host host = new Host(address, startPort, endPort);
            HostConnection connection = new HostConnection(socket, writer, reader);
            dataStore.registerHost(host);
            dataStore.registerActiveHostConnection(host.getId(), connection);
            writer.println("OK");
            this.keepConnectionAlive = true; // CRITICAL: Signal to not close this connection.
        } else {
            writer.println("ERROR Invalid code");
        }
    }

    private void handleCreateWorkspace(String[] parts) {
        String[] args = parts[1].split(" ");
        String phone = args[0], password = args[1], workspaceName = args[2];

        if (dataStore.findUser(phone).filter(u -> u.getPassword().equals(password)).isEmpty()) {
            writer.println("ERROR Invalid credentials"); return;
        }
        if (dataStore.isWorkspaceNameTaken(workspaceName)) {
            writer.println("ERROR Workspace name is already taken"); return;
        }

        Optional<Host> hostOpt = dataStore.getRandomAvailableHost();
        if (hostOpt.isEmpty()) {
            writer.println("ERROR No available hosts"); return;
        }
        Host host = hostOpt.get();
        int port = host.getAvailablePort();
        if (port == -1) {
            writer.println("ERROR Selected host has no available ports"); return;
        }

        Optional<HostConnection> connOpt = dataStore.getActiveHostConnection(host.getId());
        if (connOpt.isEmpty()) {
            writer.println("ERROR Host is registered but its connection is lost.");
            dataStore.removeHost(host.getId()); // Cleanup inconsistent state
            return;
        }

        HostConnection hostConnection = connOpt.get();
        // Synchronize on the connection object to ensure thread safety
        synchronized (hostConnection) {
            try {
                log.debug("Sending create-workspace command to host {}", host.getId());
                hostConnection.getWriter().println("create-workspace " + port + " " + phone);

                // Block and wait for the host's response on the persistent connection
                String hostResponse = hostConnection.getReader().readLine();
                log.debug("Received response from host {}: {}", host.getId(), hostResponse);

                if (hostResponse != null && hostResponse.equalsIgnoreCase("OK")) {
                    Workspace workspace = new Workspace(workspaceName, host, port, phone);
                    dataStore.createWorkspace(workspace);
                    writer.println("OK " + host.getAddress() + " " + port);
                    log.info("Successfully created workspace '{}' on host {}", workspaceName, host.getId());
                } else {
                    writer.println("ERROR Host failed to create workspace.");
                    log.warn("Host {} failed to create workspace on port {}", host.getId(), port);
                }
            } catch (IOException e) {
                writer.println("ERROR Communication with host failed.");
                log.error("Lost connection to host {} during create-workspace.", host.getId(), e);
                dataStore.removeHost(host.getId()); // The host is offline, remove it.
            }
        }
    }

    // Other handlers (register, login, connect-workspace, whois) remain largely the same

    private void handleRegister(String[] parts) {
        if (parts.length != 2) {
            writer.println("ERROR Invalid command format. Use: register <phone> <password>");
            return;
        }
        String[] args = parts[1].split(" ");
        if (args.length != 2) {
            writer.println("ERROR Invalid command format. Use: register <phone> <password>");
            return;
        }

        String phone = args[0];
        String password = args[1];

        if (dataStore.isUserRegistered(phone)) {
            writer.println("ERROR User already exists");
            log.warn("Registration failed for existing user: {}", phone);
        } else {
            User newUser = new User(phone, password);
            dataStore.registerUser(newUser);
            writer.println("OK");
            log.info("Successfully registered user: {}", phone);
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length != 2) {
            writer.println("ERROR Invalid command format. Use: login <phone> <password>");
            return;
        }
        String[] args = parts[1].split(" ");
        if (args.length != 2) {
            writer.println("ERROR Invalid command format. Use: login <phone> <password>");
            return;
        }
        String phone = args[0];
        String password = args[1];

        dataStore.findUser(phone)
                .filter(user -> user.getPassword().equals(password))
                .ifPresentOrElse(
                        user -> {
                            writer.println("OK");
                            log.info("User login successful: {}", phone);
                        },
                        () -> {
                            writer.println("ERROR Invalid credentials");
                            log.warn("User login failed for: {}", phone);
                        }
                );
    }

    private void handleConnectWorkspace(String[] parts) {
        // فرمت مورد انتظار: connect-workspace <phone> <password> <workspace_name>
        if (parts.length != 2) {
            writer.println("ERROR Invalid command format."); return;
        }
        String[] args = parts[1].split(" ");
        if (args.length != 3) {
            writer.println("ERROR Invalid command format."); return;
        }
        String phone = args[0], password = args[1], workspaceName = args[2];

        // ۱. اعتبارسنجی کاربر
        if (dataStore.findUser(phone).filter(u -> u.getPassword().equals(password)).isEmpty()) {
            writer.println("ERROR Invalid credentials"); return;
        }

        // ۲. پیدا کردن فضای کار
        Optional<Workspace> wsOpt = dataStore.findWorkspace(workspaceName);
        if (wsOpt.isEmpty()) {
            writer.println("ERROR Workspace not found"); return;
        }
        Workspace ws = wsOpt.get();

        // ۳. ساخت توکن موقت
        String tokenValue = generateRandomToken();
        Token token = new Token(tokenValue, phone);
        dataStore.saveToken(token);

        // ۴. ارسال پاسخ به کلاینت
        writer.println("OK " + ws.getHost().getAddress() + " " + ws.getPort() + " " + tokenValue);
        log.info("Issued token for user {} to connect to workspace {}", phone, workspaceName);
    }

    private void handleWhois(String[] parts) {
        // این دستور از طرف میزبان برای تایید توکن کلاینت ارسال می‌شود
        // فرمت: whois <token>
        if (parts.length != 2) {
            writer.println("ERROR Invalid command format. Use: whois <token>");
            return;
        }
        String tokenValue = parts[1];
        dataStore.findAndInvalidateToken(tokenValue)
                .ifPresentOrElse(
                        token -> {
                            writer.println("OK " + token.getPhoneNumber());
                            log.info("Token {} validated for user {}", token.getValue(), token.getPhoneNumber());
                        },
                        () -> {
                            writer.println("ERROR Invalid or expired token");
                            log.warn("Token validation failed for: {}", tokenValue);
                        }
                );
    }
    private String generateRandomToken() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder(10);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        return token.toString();
    }
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log.debug("Connection closed for {}", socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            log.error("Error while closing connection: {}", e.getMessage());
        }
    }
}

