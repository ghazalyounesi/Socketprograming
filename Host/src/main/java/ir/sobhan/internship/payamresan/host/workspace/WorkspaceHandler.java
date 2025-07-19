package ir.sobhan.internship.payamresan.host.workspace;

import ir.sobhan.internship.payamresan.host.connector.CentralServerConnector;
import ir.sobhan.internship.payamresan.host.workspace.model.ConnectedClient;
import ir.sobhan.internship.payamresan.host.workspace.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import ir.sobhan.internship.payamresan.host.workspace.model.WorkspaceState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WorkspaceHandler implements Runnable {

    private final int port;
    private final String creatorPhone;
    private final CentralServerConnector serverConnector;
    private final ExecutorService clientThreadPool;
    private final ConcurrentHashMap<String, List<Message>> conversations;
    private final ConcurrentHashMap<String, AtomicInteger> sequenceCounters;
    private final ConcurrentHashMap<String, Integer> lastReadSequence;
    // --- این فیلدها به درستی در این کلاس قرار دارند ---
    private final ConcurrentHashMap<String, ConnectedClient> connectedClientsByUsername = new ConcurrentHashMap<>();

    public WorkspaceHandler(int port, String creatorPhone, CentralServerConnector serverConnector) {
        this.port = port;
        this.creatorPhone = creatorPhone;
        this.serverConnector = serverConnector;
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.conversations = new ConcurrentHashMap<>();
        this.sequenceCounters = new ConcurrentHashMap<>();
        this.lastReadSequence = new ConcurrentHashMap<>();
    }
    public WorkspaceHandler(WorkspaceState state, CentralServerConnector serverConnector) {
        this.port = state.getPort();
        this.creatorPhone = state.getCreatorPhone();
        this.serverConnector = serverConnector;
        this.clientThreadPool = Executors.newCachedThreadPool();
        // بارگذاری داده‌های قبلی
        this.conversations = state.getConversations();
        this.sequenceCounters = state.getSequenceCounters();
        this.lastReadSequence = state.getLastReadSequence();
        log.info("Workspace on port {} restored from saved state.", this.port);
    }
    public WorkspaceState getPersistentState() {
        WorkspaceState state = new WorkspaceState();
        state.setPort(this.port);
        state.setCreatorPhone(this.creatorPhone);
        state.setConversations(this.conversations);
        state.setSequenceCounters(this.sequenceCounters);
        state.setLastReadSequence(this.lastReadSequence);
        return state;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Workspace is now running and listening on port {}", port);
            WorkspaceManager.notifyWorkspaceStarted(port);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientConnectionHandler clientHandler = new ClientConnectionHandler(clientSocket, this, serverConnector);
                    clientThreadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) break;
                    log.error("Error accepting client connection on port {}", port, e);
                }
            }
        } catch (IOException e) {
            log.error("FATAL: Could not start workspace server on port {}.", port, e);
            WorkspaceManager.notifyWorkspaceFailed(port);
        } finally {
            clientThreadPool.shutdownNow();
            log.info("Workspace on port {} has shut down.", port);
        }
    }

    public synchronized boolean registerAuthenticatedClient(ConnectedClient client) {
        if (connectedClientsByUsername.containsKey(client.getUsername())) {
            return false;
        }
        connectedClientsByUsername.put(client.getUsername(), client);
        log.info("User '{}' successfully joined the workspace on port {}.", client.getUsername(), port);
        return true;
    }

    public void removeClient(ConnectedClient client) {
        if (client != null && client.getUsername() != null) {
            connectedClientsByUsername.remove(client.getUsername());
            log.info("User '{}' has left the workspace on port {}.", client.getUsername(), port);
        }
    }

    /**
     * (این متد به درستی در این کلاس قرار دارد) - منطق اصلی برای پردازش دستور send-message.
     */
    public synchronized void handleSendMessage(ConnectedClient sender, String recipientUsername, String messageJson) {
        ConnectedClient recipient = connectedClientsByUsername.get(recipientUsername);
        String conversationId = createConversationId(sender.getUsername(), recipientUsername);
        sequenceCounters.putIfAbsent(conversationId, new AtomicInteger(0));
        int seq = sequenceCounters.get(conversationId).incrementAndGet();

        try {
            JSONObject json = new JSONObject(messageJson);
            Message message = new Message(seq, sender.getUsername(), json.getString("type"), json.getString("body"));

            conversations.putIfAbsent(conversationId, Collections.synchronizedList(new ArrayList<>()));
            conversations.get(conversationId).add(message);

            // --- تغییر کلیدی ---
            // فرستنده پیام، همیشه آخرین پیام گفتگوی خودش را "خوانده" است.
            String senderLastReadKey = sender.getUsername() + "-" + conversationId;
            lastReadSequence.put(senderLastReadKey, seq);

            if (recipient != null) {
                String messageToSend = String.format("receive-message %s %s", sender.getUsername(), message.toJsonString());
                recipient.getWriter().println(messageToSend);
            } else {
                log.warn("Recipient '{}' is not online. Message will be stored.", recipientUsername);
            }
            sender.getWriter().println("OK " + seq);
        } catch (Exception e) {
            log.error("Error parsing message JSON from user {}", sender.getUsername(), e);
            sender.getWriter().println("ERROR: Invalid JSON format.");
        }
    }


    private String createConversationId(String user1, String user2) {
        List<String> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        Collections.sort(users);
        return String.join("-", users);
    }

    public void handleGetChats(ConnectedClient requester) {
        JSONArray chatsArray = new JSONArray();
        String requesterUsername = requester.getUsername();

        for (String conversationId : conversations.keySet()) {
            if (conversationId.contains(requesterUsername)) {
                String[] users = conversationId.split("-");
                String otherUsername = users[0].equals(requesterUsername) ? users[1] : users[0];

                List<Message> messages = conversations.get(conversationId);
                int totalMessages = messages.size();
                String lastReadKey = requesterUsername + "-" + conversationId;
                int lastReadSeq = lastReadSequence.getOrDefault(lastReadKey, 0);
                int unreadCount = totalMessages - lastReadSeq;

                JSONObject chatInfo = new JSONObject();
                chatInfo.put("name", otherUsername);
                chatInfo.put("unread_count", unreadCount);
                // --- تغییر کلیدی ---
                chatInfo.put("total_messages", totalMessages); // اضافه کردن تعداد کل پیام‌ها

                chatsArray.put(chatInfo);
            }
        }
        requester.getWriter().println("OK " + chatsArray.toString());
        log.debug("Sent chat list to user '{}'", requesterUsername);
    }
public void handleGetMessages(ConnectedClient requester, String otherUsername) {
    String conversationId = createConversationId(requester.getUsername(), otherUsername);
    List<Message> messages = conversations.get(conversationId);

    JSONArray messagesArray = new JSONArray();
    if (messages != null && !messages.isEmpty()) {
        // تمام پیام‌های گفتگو را به آرایه JSON اضافه می‌کنیم
        for (Message msg : messages) {
            messagesArray.put(new JSONObject(msg.toJsonString()));
        }

        // --- تغییر کلیدی ---
        // گفتگو را به عنوان "خوانده شده" علامت می‌زنیم
        int lastSeqInChat = messages.get(messages.size() - 1).getSeq();
        String lastReadKey = requester.getUsername() + "-" + conversationId;
        lastReadSequence.put(lastReadKey, lastSeqInChat);
        log.debug("Marked conversation '{}' as read for user '{}' up to seq {}.", conversationId, requester.getUsername(), lastSeqInChat);
    }

    // ارسال لیست پیام‌ها به کلاینت
    requester.getWriter().println("OK " + messagesArray.toString());
    log.debug("Sent message history for chat with '{}' to user '{}'", otherUsername, requester.getUsername());
}
}




