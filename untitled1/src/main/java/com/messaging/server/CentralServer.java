package com.messaging.server;

import com.messaging.server.handlers.ConnectionHandler;
import com.messaging.server.storage.DataStore;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CentralServer {

    private static final int PORT = 8000;
    private static final int THREAD_POOL_SIZE = 50;
    private static final String DATA_FILE_PATH = "central_server_data.json";
    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        // ۱. بارگذاری داده‌های قبلی در زمان راه‌اندازی
        DataStore.getInstance().loadStateFromFile(DATA_FILE_PATH);

        // ۲. راه‌اندازی یک Thread جدید برای گوش دادن به دستور shutdown
        Thread shutdownListener = new Thread(() -> {
            log.info("Type 'shutdown' in this console to save data and stop the server.");
            try (Scanner scanner = new Scanner(System.in)) {
                // این حلقه فقط برای خواندن دستور shutdown است
                while (true) {
                    if (scanner.hasNextLine()) {
                        if ("shutdown".equalsIgnoreCase(scanner.nextLine().trim())) {
                            isRunning = false;
                            break; // از حلقه شنونده خارج می‌شویم
                        }
                    }
                }
            }
        });
        shutdownListener.setName("Shutdown-Listener");
        shutdownListener.setDaemon(true); // این Thread با بسته شدن برنامه اصلی، بسته می‌شود
        shutdownListener.start();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("Central Server is running and listening on port {}", PORT);

            while (isRunning) {
                try {
                    // یک timeout کوتاه به accept اضافه می‌کنیم تا حلقه بتواند وضعیت isRunning را چک کند.
                    serverSocket.setSoTimeout(1000);
                    Socket clientSocket = serverSocket.accept();
                    // فقط در صورتی که سرور هنوز در حال اجرا باشد، اتصال جدید را پردازش کن
                    if (isRunning) {
                        log.info("Accepted new connection from {}", clientSocket.getRemoteSocketAddress());
                        executorService.submit(new ConnectionHandler(clientSocket));
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // این خطا طبیعی است و فقط برای این است که حلقه بتواند isRunning را چک کند.
                    // نیازی به انجام کاری نیست.
                } catch (IOException e) {
                    if (isRunning) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("FATAL: Could not start server on port {}", PORT, e);
        } finally {
            log.info("Server is shutting down...");
            executorService.shutdown();
            // ۳. ذخیره داده‌ها قبل از خروج کامل
            DataStore.getInstance().saveStateToFile(DATA_FILE_PATH);
            log.info("Central Server has shut down completely.");
            // این خط تضمین می‌کند که برنامه به طور کامل بسته شود
            System.exit(0);
        }
    }
}
