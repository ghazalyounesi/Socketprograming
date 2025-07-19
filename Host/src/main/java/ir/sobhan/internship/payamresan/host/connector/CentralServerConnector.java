
package ir.sobhan.internship.payamresan.host.connector;

import ir.sobhan.internship.payamresan.host.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class CentralServerConnector implements Runnable {

    private final String centralServerIp;
    private final int centralServerPort;
    private final String hostIp;
    private final int hostStartPort;
    private final int hostEndPort;
    private final WorkspaceManager workspaceManager;

    private Socket centralServerSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    public CentralServerConnector(String centralServerIp, int centralServerPort, String hostIp, int hostStartPort, int hostEndPort, WorkspaceManager workspaceManager) {
        this.centralServerIp = centralServerIp;
        this.centralServerPort = centralServerPort;
        this.hostIp = hostIp;
        this.hostStartPort = hostStartPort;
        this.hostEndPort = hostEndPort;
        this.workspaceManager = workspaceManager;
    }

    @Override
    public void run() {
        try {

            centralServerSocket = new Socket(centralServerIp, centralServerPort);
            writer = new PrintWriter(centralServerSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(centralServerSocket.getInputStream()));
            log.info("Successfully connected to Central Server at {}:{}", centralServerIp, centralServerPort);

            if (performHandshake()) {
                log.info("Host successfully registered with the Central Server.");

                listenForCommands();
            } else {
                log.error("Handshake with Central Server failed. Shutting down.");
            }

        } catch (IOException e) {
            log.error("Could not connect to or communicate with Central Server. Please check if the server is running.", e);
        } finally {
            closeConnection();
        }
    }

    private boolean performHandshake() throws IOException {

        String createHostCommand = String.format("create-host %s %d %d", hostIp, hostStartPort, hostEndPort);
        log.debug("Sending command to server: {}", createHostCommand);
        writer.println(createHostCommand);

        String serverResponse = reader.readLine();
        log.debug("Received from server: {}", serverResponse);
        String[] parts = serverResponse.split(" ");
        if (!parts[0].equals("OK") || parts.length < 2) {
            log.error("Server did not respond with a valid port. Response: {}", serverResponse);
            return false;
        }
        int randomPort = Integer.parseInt(parts[1]);
        log.info("Server asked to listen on temporary port: {}", randomPort);


        try (ServerSocket tempSocket = new ServerSocket(randomPort)) {
            writer.println("check");
            log.debug("Sent 'check' to server. Waiting for connection on port {}", randomPort);

            try (Socket verificationConnection = tempSocket.accept()) {
                BufferedReader verificationReader = new BufferedReader(new InputStreamReader(verificationConnection.getInputStream()));
                String verificationCode = verificationReader.readLine();
                log.info("Received verification code: {}", verificationCode);


                writer.println(verificationCode);
                log.debug("Sent verification code back to server.");

                String finalResponse = reader.readLine();
                log.debug("Final response from server: {}", finalResponse);
                return finalResponse.equals("OK");
            }
        }
    }

    private void listenForCommands() {
        log.info("Host is now listening for commands from the Central Server...");
        try {
            String commandFromServer;
            while ((commandFromServer = reader.readLine()) != null) {
                log.info("Received command from Central Server: {}", commandFromServer);

                String[] parts = commandFromServer.split(" ");
                String commandType = parts[0];

                if ("create-workspace".equalsIgnoreCase(commandType)) {
                    if (parts.length == 3) {
                        int port = Integer.parseInt(parts[1]);
                        String creatorPhone = parts[2];

                        boolean success = workspaceManager.createAndStartWorkspace(port, creatorPhone, this);

                        if (success) {
                            writer.println("OK");
                            log.info("Successfully created workspace on port {} and sent OK to server.", port);
                        } else {
                            writer.println("ERROR Host failed to create workspace");
                            log.error("Failed to create workspace on port {}. Sent ERROR to server.", port);
                        }
                    } else {
                        log.warn("Received malformed create-workspace command: {}", commandFromServer);
                    }
                } else {
                    log.warn("Received unknown command from server: {}", commandFromServer);
                }
            }
        } catch (IOException e) {
            log.error("Connection to Central Server lost.", e);
        } catch (NumberFormatException e) {
            log.error("Received invalid port number in command from server.", e);
        }
    }

    public String verifyTokenAndGetUserPhone(String token) {
        log.debug("Opening a new temporary connection to verify token: {}", token);

        try (Socket tempSocket = new Socket(centralServerIp, centralServerPort)) {
            PrintWriter tempWriter = new PrintWriter(tempSocket.getOutputStream(), true);
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

            String command = "whois " + token;
            log.debug("Sending token verification command to central server: {}", command);
            tempWriter.println(command);

            String response = tempReader.readLine();
            log.debug("Received response for token verification: {}", response);

            if (response != null && response.startsWith("OK")) {
                String[] parts = response.split(" ");
                if (parts.length == 2) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            log.error("Failed to communicate with central server for token verification.", e);
        }
        return null;
    }


    private void closeConnection() {
        try {
            if (centralServerSocket != null && !centralServerSocket.isClosed()) {
                centralServerSocket.close();
            }
            log.info("Connection to Central Server closed.");
        } catch (IOException e) {
            log.error("Error while closing connection.", e);
        }
    }
}
