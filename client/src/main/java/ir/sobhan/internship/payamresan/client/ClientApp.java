package ir.sobhan.internship.payamresan.client;

import lombok.extern.slf4j.Slf4j;
import java.util.Scanner;

@Slf4j
public class ClientApp {

    public static void main(String[] args) {
        log.info("Client Application Started.");
        log.info("Available commands: register, login, logout, create-workspace, connect-workspace, send-message, get-chats, get-messages, disconnect, exit");

        String centralServerIp = "127.0.0.1";
        int centralServerPort = 8000;

        ConnectionManager connectionManager = new ConnectionManager(centralServerIp, centralServerPort);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine();
            if (commandLine.trim().isEmpty()) {
                continue;
            }
            String[] parts = commandLine.split(" ", 2);
            String command = parts[0].toLowerCase();

            if ("exit".equalsIgnoreCase(command)) {
                log.info("Exiting application...");
                connectionManager.disconnect();
                break;
            }

            switch (command) {
                case "register":
                    connectionManager.register(commandLine);
                case "login":
                    connectionManager.login(commandLine);
                    break;
                case "logout":
                    connectionManager.logout();
                case "create-workspace":
                    connectionManager.createWorkspace(commandLine);
                    break;
                case "connect-workspace":
                    connectionManager.connectToWorkspace(commandLine);
                    break;
                case "disconnect":
                    connectionManager.disconnect();
                    break;
                case "send-message":
                    connectionManager.sendCommandToWorkspace(commandLine);
                    break;
                case "get-chats":
                    connectionManager.sendCommandToWorkspace(commandLine);
                    break;
                case "get-messages":
                    connectionManager.sendCommandToWorkspace(commandLine);
                    break;
                default:
                    System.out.println("Unknown command. Please try again.");
            }
        }
        scanner.close();
        log.info("Client Application Terminated.");
    }
}
