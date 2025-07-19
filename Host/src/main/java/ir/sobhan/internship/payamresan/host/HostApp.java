package ir.sobhan.internship.payamresan.host;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import java.util.Scanner;

@Slf4j
public class HostApp {

    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        if (args.length < 5) {
            log.error("Insufficient arguments. Usage: java HostApp <central_ip> <central_port> <host_ip> <start_port> <end_port>");
            return;
        }

        String centralServerIp = args[0];
        int centralServerPort = Integer.parseInt(args[1]);
        String hostIp = args[2];
        int hostStartPort = Integer.parseInt(args[3]);
        String dataFilePath = "host_data_" + hostStartPort + ".json";

        WorkspaceManager workspaceManager = new WorkspaceManager();
        CentralServerConnector connector = new CentralServerConnector(centralServerIp, centralServerPort, hostIp, hostStartPort, Integer.parseInt(args[4]), workspaceManager);

        workspaceManager.loadAndRestartWorkspaces(dataFilePath, connector);

        Thread connectorThread = new Thread(connector);
        connectorThread.setName("CentralServer-Connector-Thread");
        connectorThread.start();

        Thread shutdownListener = new Thread(() -> {
            log.info("Type 'shutdown' in this console to save data and stop the host.");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    if (scanner.hasNextLine() && "shutdown".equalsIgnoreCase(scanner.nextLine().trim())) {
                        isRunning = false;
                        break;
                    }
                }
            }
        });
        shutdownListener.setDaemon(true);
        shutdownListener.start();

        log.info("Host application is now running.");

        try {
            while (isRunning) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            log.info("Shutting down...");
            workspaceManager.saveAllWorkspaces(dataFilePath);
            System.exit(0);
        }
    }
}
