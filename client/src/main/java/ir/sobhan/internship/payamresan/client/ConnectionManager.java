package ir.sobhan.internship.payamresan.client;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Slf4j
public class ConnectionManager {

    private final String centralServerIp;
    private final int centralServerPort;
    private String loggedInUserPhone;
    private String loggedInUserPassword;
    private Socket workspaceSocket;
    private PrintWriter workspaceWriter;
    private BufferedReader workspaceReader;
    private Thread listenerThread;

    public ConnectionManager(String centralServerIp, int centralServerPort) {
        this.centralServerIp = centralServerIp;
        this.centralServerPort = centralServerPort;
    }
    public boolean isLoggedIn() {
        return loggedInUserPhone != null && loggedInUserPassword != null;
    }

    private String sendCommandToCentralServer(String command) {
        try (Socket socket = new Socket(centralServerIp, centralServerPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            log.debug("Sending to Central Server: {}", command);
            writer.println(command);
            String response = reader.readLine();
            log.debug("Received from Central Server: {}", response);
            return response;

        } catch (IOException e) {
            log.error("Cannot connect to central server: {}", e.getMessage());
            return "ERROR: Cannot connect to central server.";
        }
    }

    public void register(String command) {
        String response = sendCommandToCentralServer(command);
        System.out.println(response);
    }
    public void login(String command) {
        if (isLoggedIn()) {
            System.out.println("ERROR: You are already logged in as " + loggedInUserPhone);
            return;
        }
        String[] parts = command.split(" ");
        if (parts.length != 3) {
            System.out.println("ERROR: Invalid format. Use: login <phone> <password>");
            return;
        }

        String response = sendCommandToCentralServer(command);
        System.out.println(response);

        if (response != null && response.equalsIgnoreCase("OK")) {
            this.loggedInUserPhone = parts[1];
            this.loggedInUserPassword = parts[2];
            log.info("User {} successfully logged in.", loggedInUserPhone);
        }
    }

    public void logout() {
        if (!isLoggedIn()) {
            System.out.println("ERROR: You are not logged in.");
            return;
        }
        disconnect();
        this.loggedInUserPhone = null;
        this.loggedInUserPassword = null;
        System.out.println("Successfully logged out.");
    }
    public void createWorkspace(String command) {
        if (!isLoggedIn()) {
            System.out.println("ERROR: Please log in first.");
            return;
        }
        String[] parts = command.split(" ");
        if (parts.length != 2) {
            System.out.println("ERROR: Invalid format. Use: create-workspace <workspace_name>");
            return;
        }
        String workspaceName = parts[1];
        String fullCommand = String.format("create-workspace %s %s %s", loggedInUserPhone, loggedInUserPassword, workspaceName);

        String response = sendCommandToCentralServer(fullCommand);
        System.out.println(response);
    }


    public void connectToWorkspace(String command) {
        if (!isLoggedIn()) {
            System.out.println("ERROR: Please log in first.");
            return;
        }
        if (workspaceSocket != null && !workspaceSocket.isClosed()) {
            System.out.println("ERROR: You are already connected to a workspace. Please disconnect first.");
            return;
        }

        String[] parts = command.split(" ");
        if (parts.length != 2) {
            System.out.println("ERROR: Invalid format. Use: connect-workspace <workspace_name>");
            return;
        }
        String workspaceName = parts[1];
        String fullCommand = String.format("connect-workspace %s %s %s", loggedInUserPhone, loggedInUserPassword, workspaceName);

        String response = sendCommandToCentralServer(fullCommand);

    }

    public void disconnect() {
        if (workspaceSocket == null || workspaceSocket.isClosed()) {
            System.out.println("You are not connected to any workspace.");
            return;
        }
        try {
            workspaceSocket.close();
        } catch (IOException e) {
            log.error("Error while closing socket: {}", e.getMessage());
        } finally {
            workspaceSocket = null;
            workspaceWriter = null;
            workspaceReader = null;
            listenerThread = null;
            System.out.println("Disconnected from workspace.");
        }
    }
    public void sendCommandToWorkspace(String command) {
        if (workspaceSocket != null && !workspaceSocket.isClosed()) {
            workspaceWriter.println(command);
            log.debug("Sent to Workspace: {}", command);
        } else {
            System.out.println("ERROR: You are not connected to any workspace.");
        }
    }
}
