package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import lombok.extern.slf4j.Slf4j;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import ir.sobhan.internship.payamresan.host.workspace.model.WorkspaceState;
import ir.sobhan.internship.payamresan.host.workspace.model.HostPersistentState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Slf4j
public class WorkspaceManager {
    private final ConcurrentHashMap<Integer, WorkspaceHandler> runningWorkspaces = new ConcurrentHashMap<>();
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

        runningWorkspaces.put(port, workspaceHandler);
        try {
            // منتظر می‌مانیم تا WorkspaceHandler به ما خبر دهد (حداکثر ۵ ثانیه)
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
    public synchronized void saveAllWorkspaces(String filePath) {
        if (runningWorkspaces.isEmpty()) {
            log.info("No active workspaces to save.");
            return;
        }
        log.info("Attempting to save state of {} active workspaces to {}...", runningWorkspaces.size(), filePath);
        HostPersistentState state = new HostPersistentState();
        ConcurrentHashMap<Integer, WorkspaceState> workspaceStates = new ConcurrentHashMap<>();

        runningWorkspaces.forEach((port, handler) -> {
            workspaceStates.put(port, handler.getPersistentState());
        });
        state.setRunningWorkspaces(workspaceStates);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(state, writer);
            log.info("Host state successfully saved.");
        } catch (IOException e) {
            log.error("Failed to save host state to file.", e);
        }
    }
    public synchronized void loadAndRestartWorkspaces(String filePath, CentralServerConnector serverConnector) {
        log.info("Attempting to load host state from {}...", filePath);
        Gson gson = new Gson();
        try (Reader reader = new FileReader(filePath)) {
            HostPersistentState loadedState = gson.fromJson(reader, HostPersistentState.class);
            if (loadedState != null && loadedState.getRunningWorkspaces() != null) {
                log.info("Found {} saved workspaces. Attempting to restart them...", loadedState.getRunningWorkspaces().size());
                loadedState.getRunningWorkspaces().forEach((port, wsState) -> {
                    log.debug("Restarting workspace on port {}", port);
                    WorkspaceHandler handler = new WorkspaceHandler(wsState, serverConnector);
                    Thread thread = new Thread(handler);
                    thread.setName("Workspace-Port-" + port);
                    thread.start();
                    runningWorkspaces.put(port, handler);
                });
            }
        } catch (IOException e) {
            log.warn("No existing host data file found at '{}'. Starting fresh.", filePath);
        } catch (Exception e) {
            log.error("Failed to load or parse host state from file.", e);
        }
    }
}