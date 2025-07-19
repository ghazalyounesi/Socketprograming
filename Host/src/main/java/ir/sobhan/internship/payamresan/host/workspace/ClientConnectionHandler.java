package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.model.ConnectedClient;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Slf4j
public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final WorkspaceHandler workspaceHandler;
    private final CentralServerConnector serverConnector;
    private ConnectedClient client;
    private PrintWriter writer;
    private BufferedReader reader;

    public ClientConnectionHandler(Socket socket, WorkspaceHandler workspaceHandler, CentralServerConnector serverConnector) {
        this.socket = socket;
        this.workspaceHandler = workspaceHandler;
        this.serverConnector = serverConnector;
    }

    @Override
    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // ۱. انجام فرآیند اتصال و تایید هویت
            if (!handleClientConnection()) {
                // اگر اتصال ناموفق بود، از متد خارج شده و Thread تمام می‌شود.
                return;
            }

            // ۲. گوش دادن به دستورات بعدی کلاینت (مثل send-message)
            listenForClientCommands();

        } catch (IOException e) {
            log.warn("Connection with client lost: {}", e.getMessage());
        } finally {
            // ۳. پاک‌سازی نهایی
            workspaceHandler.removeClient(this.client);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("Error closing client socket.", e);
            }
            log.info("Cleaned up resources for client '{}'.", (client != null ? client.getUsername() : "UNKNOWN"));
        }
    }

    private boolean handleClientConnection() throws IOException {
        // مرحله ۱: خواندن دستور connect <token>
        String connectCommand = reader.readLine();
        if (connectCommand == null || !connectCommand.toLowerCase().startsWith("connect ")) {
            writer.println("ERROR Invalid initial command. Expected 'connect <token>'.");
            return false;
        }

        // مرحله ۲: اعتبارسنجی توکن
        String token = connectCommand.split(" ")[1];
        String phoneNumber = serverConnector.verifyTokenAndGetUserPhone(token);

        if (phoneNumber == null) {
            writer.println("ERROR Invalid or expired token.");
            return false;
        }

        // مرحله ۳: درخواست نام کاربری
        writer.println("username?");
        String username = reader.readLine();

        if (username == null || username.trim().isEmpty()) {
            writer.println("ERROR Username cannot be empty.");
            return false;
        }

        // مرحله ۴: ثبت نهایی کلاینت در فضای کار
        this.client = new ConnectedClient(phoneNumber, socket, writer);
        this.client.setUsername(username);

        if (!workspaceHandler.registerAuthenticatedClient(this.client)) {
            writer.println("ERROR Username is already taken.");
            return false;
        }

        // مرحله ۵: ارسال تاییدیه نهایی به کلاینت
        writer.println("OK");
        return true;
    }

    private void listenForClientCommands() throws IOException {
        String clientCommand;
        while ((clientCommand = reader.readLine()) != null) {
            log.debug("Received command from user '{}': {}", client.getUsername(), clientCommand);

            String[] parts = clientCommand.split(" ", 3);
            String commandType = parts[0].toLowerCase();

            switch (commandType) {
                case "disconnect":
                    log.info("User '{}' requested to disconnect.", client.getUsername());
                    return; // خروج از متد و بستن اتصال در بلوک finally

                case "send-message":
                    if (parts.length == 3) {
                        String recipient = parts[1];
                        String jsonMessage = parts[2];
                        workspaceHandler.handleSendMessage(this.client, recipient, jsonMessage);
                    } else {
                        writer.println("ERROR: Invalid send-message format.");
                    }
                    break;
                case "get-chats":
                    // --- تغییر کلیدی ---
                    // حالا چک می‌کنیم که دستور get-chats هیچ پارامتر اضافه‌ای نداشته باشد.
                    if (parts.length == 1) {
                        workspaceHandler.handleGetChats(this.client);
                    } else {
                        writer.println("ERROR: Invalid get-chats format. Use: get-chats");
                    }
                    break;

                case "get-messages":
                    if (parts.length == 2) {
                        String otherUsername = parts[1];
                        workspaceHandler.handleGetMessages(this.client, otherUsername);
                    } else {
                        writer.println("ERROR: Invalid get-messages format. Use: get-messages <username>");
                    }
                    break;
                default:
                    writer.println("ERROR: Unknown command.");
            }
        }
    }
}

