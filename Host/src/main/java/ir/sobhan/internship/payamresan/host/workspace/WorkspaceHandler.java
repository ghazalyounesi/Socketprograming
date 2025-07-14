package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.model.ConnectedClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class WorkspaceHandler implements Runnable {

    private final int port;
    private final String creatorPhone;
    private final CentralServerConnector serverConnector; // برای اعتبارسنجی توکن
    private final ExecutorService clientThreadPool;
    private final ConcurrentHashMap<String, ConnectedClient> connectedClientsByUsername = new ConcurrentHashMap<>();

    public WorkspaceHandler(int port, String creatorPhone, CentralServerConnector serverConnector) {
        this.port = port;
        this.creatorPhone = creatorPhone;
        this.serverConnector = serverConnector;
        this.clientThreadPool = Executors.newCachedThreadPool(); // یک استخر نخ برای مدیریت کلاینت‌ها
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Workspace is now running and listening on port {}", port);

            // این متد به WorkspaceManager اطلاع می‌دهد که سرور با موفقیت راه افتاده است.
            // این یک راه ساده برای هماهنگی است.
            WorkspaceManager.notifyWorkspaceStarted(port);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Accepted a new client connection on port {} from {}", port, clientSocket.getRemoteSocketAddress());
                    ClientConnectionHandler clientHandler = new ClientConnectionHandler(clientSocket, this, serverConnector);
                    clientThreadPool.submit(clientHandler);

                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("Workspace on port {} is shutting down.", port);
                    } else {
                        log.error("Error accepting client connection on port {}", port, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("FATAL: Could not start workspace server on port {}. The port might be in use.", port, e);
            // در صورت بروز خطا، به مدیر اطلاع می‌دهیم.
            WorkspaceManager.notifyWorkspaceFailed(port);
        } finally {
            clientThreadPool.shutdownNow();
        }
    }
    public synchronized boolean registerAuthenticatedClient(ConnectedClient client) {
        if (connectedClientsByUsername.containsKey(client.getUsername())) {
            return false; // نام کاربری تکراری است
        }
        connectedClientsByUsername.put(client.getUsername(), client);
        log.info("User '{}' (Phone: {}) successfully joined the workspace on port {}.", client.getUsername(), client.getPhoneNumber(), port);
        return true;
    }

    public void removeClient(ConnectedClient client) {
        if (client != null && client.getUsername() != null) {
            connectedClientsByUsername.remove(client.getUsername());
            log.info("User '{}' has left the workspace on port {}.", client.getUsername(), port);
        }
    }
}

