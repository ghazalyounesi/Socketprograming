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

    // اطلاعات اتصال دائمی به فضای کار
    private Socket workspaceSocket;
    private PrintWriter workspaceWriter;
    private BufferedReader workspaceReader;
    private Thread listenerThread;

    public ConnectionManager(String centralServerIp, int centralServerPort) {
        this.centralServerIp = centralServerIp;
        this.centralServerPort = centralServerPort;
    }

    /**
     * یک دستور را به سرور مرکزی ارسال کرده و پاسخ را برمی‌گرداند.
     * این متد برای اتصالات کوتاه‌مدت استفاده می‌شود.
     */
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

    public void createWorkspace(String command) {
        String response = sendCommandToCentralServer(command);
        System.out.println(response);
    }

    public void connectToWorkspace(String command) {
        if (workspaceSocket != null && !workspaceSocket.isClosed()) {
            System.out.println("ERROR: You are already connected to a workspace. Please disconnect first.");
            return;
        }

        // ۱. ابتدا اطلاعات اتصال و توکن را از سرور مرکزی می‌گیریم
        String response = sendCommandToCentralServer(command);
        if (response == null || !response.startsWith("OK")) {
            System.out.println(response);
            return;
        }

        // پاسخ باید فرمت: OK <ip> <port> <token> داشته باشد
        String[] parts = response.split(" ");
        if (parts.length != 4) {
            System.out.println("ERROR: Invalid response from central server.");
            return;
        }

        String wsIp = parts[1];
        int wsPort = Integer.parseInt(parts[2]);
        String token = parts[3];

        try {
            // ۲. حالا اتصال دائمی به فضای کار برقرار می‌کنیم
            workspaceSocket = new Socket(wsIp, wsPort);
            workspaceWriter = new PrintWriter(workspaceSocket.getOutputStream(), true);
            workspaceReader = new BufferedReader(new InputStreamReader(workspaceSocket.getInputStream()));
            log.info("Successfully connected to Workspace at {}:{}", wsIp, wsPort);

            // ۳. خودمان را با توکن معرفی می‌کنیم
            workspaceWriter.println("connect " + token);

            // ۴. منتظر درخواست نام کاربری می‌مانیم
            String serverPrompt = workspaceReader.readLine();
            System.out.println(serverPrompt); // "username?"

            if (serverPrompt == null || !serverPrompt.equalsIgnoreCase("username?")) {
                System.out.println("ERROR: Unexpected response from workspace.");
                disconnect();
                return;
            }

            // ۵. نام کاربری را از کاربر می‌گیریم و ارسال می‌کنیم
            System.out.print("Enter username: ");
            String username = new BufferedReader(new InputStreamReader(System.in)).readLine();
            workspaceWriter.println(username);

            // ۶. منتظر تاییدیه نهایی (OK) می‌مانیم
            String finalResponse = workspaceReader.readLine();
            System.out.println(finalResponse);

            if (finalResponse != null && finalResponse.equalsIgnoreCase("OK")) {
                // ۷. اگر همه چیز موفق بود، Thread شنونده را راه‌اندازی می‌کنیم
                listenerThread = new Thread(new ServerListener(workspaceReader));
                listenerThread.setName("Server-Listener-Thread");
                listenerThread.start();
                log.info("Listener thread started. Ready to receive messages.");
            } else {
                disconnect();
            }

        } catch (IOException e) {
            log.error("Failed to connect to workspace: {}", e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        if (workspaceSocket == null || workspaceSocket.isClosed()) {
            System.out.println("You are not connected to any workspace.");
            return;
        }
        try {
            // بستن سوکت باعث می‌شود که listenerThread هم به صورت خودکار تمام شود
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
