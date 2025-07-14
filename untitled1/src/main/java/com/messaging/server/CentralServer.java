package com.messaging.server;
import com.messaging.server.storage.DataStore;
import com.messaging.server.handlers.ConnectionHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CentralServer {

    private static final int PORT = 8000;
    private static final int THREAD_POOL_SIZE = 50;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("Central Server is running and listening on port {}", PORT);
            DataStore.getInstance(); // Initialize DataStore
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Accepted new connection from {}", clientSocket.getRemoteSocketAddress());
                    executorService.submit(new ConnectionHandler(clientSocket));
                } catch (IOException e) {
                    log.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Could not start server on port {}", PORT, e);
        } finally {
            executorService.shutdown();
        }
    }
}
