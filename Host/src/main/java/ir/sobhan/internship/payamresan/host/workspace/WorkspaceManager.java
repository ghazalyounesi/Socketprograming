/*
package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import lombok.extern.slf4j.Slf4j;
import lombok.Setter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WorkspaceManager {
    @Setter
    private CentralServerConnector serverConnector;

    private final ConcurrentHashMap<Integer, Thread> runningWorkspaces = new ConcurrentHashMap<>();

    // یک مکانیزم برای هماهنگی بین Thread ها
    private static final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> startupNotifiers = new ConcurrentHashMap<>();

    public WorkspaceManager() {

        log.info("Workspace Manager initialized.");
    }

    /**
     * یک فضای کار جدید را در یک Thread مجزا ایجاد و اجرا می‌کند.
     * این متد منتظر می‌ماند تا از موفقیت‌آمیز بودن اجرای سرور مطمئن شود.
     * @return true اگر فضای کار با موفقیت اجرا شود، در غیر این صورت false.
     *//*
    public boolean createAndStartWorkspace(int port, String creatorPhone) {
        if (serverConnector == null) {
            log.error("FATAL: ServerConnector has not been set on WorkspaceManager. Cannot create workspace.");
            return false;
        }
        if (runningWorkspaces.containsKey(port)) {
            log.warn("Attempted to create a workspace on an already used port: {}", port);
            return false;
        }

        log.info("Attempting to create and start a new workspace on port {} for user {}", port, creatorPhone);

        // یک CompletableFuture برای دریافت نتیجه از Thread جدید می‌سازیم.
        CompletableFuture<Boolean> startupResult = new CompletableFuture<>();
        startupNotifiers.put(port, startupResult);

        WorkspaceHandler workspaceHandler = new WorkspaceHandler(port, creatorPhone, this.serverConnector);
        Thread workspaceThread = new Thread(workspaceHandler);
        workspaceThread.setName("Workspace-Port-" + port);
        workspaceThread.start();

        runningWorkspaces.put(port, workspaceThread);

        try {
            // منتظر می‌مانیم تا WorkspaceHandler به ما خبر دهد (حداکثر ۵ ثانیه)
            return startupResult.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Workspace on port {} failed to start in time or was interrupted.", port, e);
            workspaceThread.interrupt(); // در صورت بروز خطا، Thread را متوقف می‌کنیم.
            return false;
        } finally {
            startupNotifiers.remove(port); // پاک‌سازی
        }
    }

    public static void notifyWorkspaceStarted(int port) {
        if (startupNotifiers.containsKey(port)) {
            startupNotifiers.get(port).complete(true);
        }
    }

    public static void notifyWorkspaceFailed(int port) {
        if (startupNotifiers.containsKey(port)) {
            startupNotifiers.get(port).complete(false);
        }
    }
}
*/
package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WorkspaceManager {

    private final ConcurrentHashMap<Integer, Thread> runningWorkspaces = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> startupNotifiers = new ConcurrentHashMap<>();

    public WorkspaceManager() {
        log.info("Workspace Manager initialized.");
    }

    /**
     * یک فضای کار جدید را در یک Thread مجزا ایجاد و اجرا می‌کند.
     * @param serverConnector یک مرجع به کانکتور اصلی برای استفاده‌های بعدی (مثل whois)
     * @return true اگر فضای کار با موفقیت اجرا شود، در غیر این صورت false.
     */
    public boolean createAndStartWorkspace(int port, String creatorPhone, CentralServerConnector serverConnector) {
        if (runningWorkspaces.containsKey(port)) {
            log.warn("Attempted to create a workspace on an already used port: {}", port);
            return false;
        }

        log.info("Attempting to create and start a new workspace on port {} for user {}", port, creatorPhone);

        CompletableFuture<Boolean> startupResult = new CompletableFuture<>();
        startupNotifiers.put(port, startupResult);

        // ما serverConnector را به WorkspaceHandler پاس می‌دهیم.
        WorkspaceHandler workspaceHandler = new WorkspaceHandler(port, creatorPhone, serverConnector);
        Thread workspaceThread = new Thread(workspaceHandler);
        workspaceThread.setName("Workspace-Port-" + port);
        workspaceThread.start();

        runningWorkspaces.put(port, workspaceThread);

        try {
            return startupResult.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Workspace on port {} failed to start in time or was interrupted.", port, e);
            workspaceThread.interrupt();
            return false;
        } finally {
            startupNotifiers.remove(port);
        }
    }

    public static void notifyWorkspaceStarted(int port) {
        if (startupNotifiers.containsKey(port)) {
            startupNotifiers.get(port).complete(true);
        }
    }

    public static void notifyWorkspaceFailed(int port) {
        if (startupNotifiers.containsKey(port)) {
            startupNotifiers.get(port).complete(false);
        }
    }
}