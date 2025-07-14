/*
package ir.sobhan.internship.payamresan.host;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostApp {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("Starting Host Application...");
        log.info("========================================");

        if (args.length < 5) {
            log.error("Insufficient arguments. Usage: java -jar host.jar <central_server_ip> <central_server_port> <host_ip> <host_start_port> <host_end_port>");
            System.exit(1);
        }

        try {
            String centralServerIp = args[0];
            int centralServerPort = Integer.parseInt(args[1]);
            String hostIp = args[2];
            int hostStartPort = Integer.parseInt(args[3]);
            int hostEndPort = Integer.parseInt(args[4]);

            log.info("Configuration loaded:");
            log.info(" -> Central Server: {}:{}", centralServerIp, centralServerPort);
            log.info(" -> This Host IP: {}", hostIp);
            log.info(" -> Port Range: {}-{}", hostStartPort, hostEndPort);

            WorkspaceManager workspaceManager = new WorkspaceManager();
            CentralServerConnector connector = new CentralServerConnector(
                    centralServerIp,
                    centralServerPort,
                    hostIp,
                    hostStartPort,
                    hostEndPort,
                    workspaceManager
            );

            Thread connectorThread = new Thread(connector);
            connectorThread.setName("CentralServer-Connector-Thread");
            connectorThread.start();

            log.info("Host application is now running. Connector thread started.");

            // --- تغییر کلیدی ---
            // متد join() باعث می‌شود که main thread منتظر بماند تا connectorThread کارش تمام شود.
            // از آنجایی که connectorThread یک حلقه بی‌نهایت دارد، main thread هم برای همیشه منتظر می‌ماند.
            // این کار از بسته شدن ناگهانی برنامه جلوگیری می‌کند.
            connectorThread.join();

        } catch (NumberFormatException e) {
            log.error("Invalid port number provided.", e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.warn("Host application main thread was interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("An unexpected error occurred during startup.", e);
            System.exit(1);
        } finally {
            log.info("Host Application is shutting down.");
        }
    }
}
*/

package ir.sobhan.internship.payamresan.host;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostApp {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("Starting Host Application...");
        log.info("========================================");

        if (args.length < 5) {
            log.error("Insufficient arguments. Usage: java -jar host.jar <central_server_ip> <central_server_port> <host_ip> <host_start_port> <host_end_port>");
            System.exit(1);
        }

        try {
            String centralServerIp = args[0];
            int centralServerPort = Integer.parseInt(args[1]);
            String hostIp = args[2];
            int hostStartPort = Integer.parseInt(args[3]);
            int hostEndPort = Integer.parseInt(args[4]);

            log.info("Configuration loaded:");
            log.info(" -> Central Server: {}:{}", centralServerIp, centralServerPort);
            log.info(" -> This Host IP: {}", hostIp);
            log.info(" -> Port Range: {}-{}", hostStartPort, hostEndPort);

            // ۱. مدیر فضاهای کاری ساخته می‌شود.
            WorkspaceManager workspaceManager = new WorkspaceManager();

            // ۲. متخصص ارتباط با سرور ساخته شده و مدیر به آن پاس داده می‌شود.
            CentralServerConnector connector = new CentralServerConnector(
                    centralServerIp,
                    centralServerPort,
                    hostIp,
                    hostStartPort,
                    hostEndPort,
                    workspaceManager
            );

            // ۳. متخصص در یک Thread جدید اجرا می‌شود.
            Thread connectorThread = new Thread(connector);
            connectorThread.setName("CentralServer-Connector-Thread");
            connectorThread.start();

            log.info("Host application is now running. Connector thread started.");
            connectorThread.join();

        } catch (Exception e) {
            log.error("An unexpected error occurred during startup.", e);
            System.exit(1);
        } finally {
            log.info("Host Application is shutting down.");
        }
    }
}
