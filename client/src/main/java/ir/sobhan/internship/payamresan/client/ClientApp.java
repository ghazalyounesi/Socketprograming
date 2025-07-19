package ir.sobhan.internship.payamresan.client;

import lombok.extern.slf4j.Slf4j;
import java.util.Scanner;

@Slf4j
public class ClientApp {

    public static void main(String[] args) {
        log.info("Client Application Started.");
        log.info("Available commands: register, create-workspace, connect-workspace, disconnect, exit");

        // آدرس سرور مرکزی را اینجا مشخص می‌کنیم
        String centralServerIp = "127.0.0.1";
        int centralServerPort = 8000;

        ConnectionManager connectionManager = new ConnectionManager(centralServerIp, centralServerPort);
        Scanner scanner = new Scanner(System.in);

        // حلقه اصلی برای دریافت دستورات از کاربر
        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine();
            String[] parts = commandLine.split(" ", 2);
            String command = parts[0].toLowerCase();

            if ("exit".equalsIgnoreCase(command)) {
                log.info("Exiting application...");
                connectionManager.disconnect();
                break;
            }

            switch (command) {
                case "register":
                case "create-workspace":
                    connectionManager.register(commandLine); // هر دو از یک نوع اتصال استفاده می‌کنند
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
                    // TODO: دستورات چت در اینجا اضافه خواهند شد
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
