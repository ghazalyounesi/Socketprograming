package ir.sobhan.internship.payamresan.host.workspace.model;

import lombok.Getter;
import java.io.PrintWriter;
import java.net.Socket;

@Getter
public class ConnectedClient {
    private final String phoneNumber;
    private final Socket socket;
    private final PrintWriter writer;
    private String username;

    public ConnectedClient(String phoneNumber, Socket socket, PrintWriter writer) {
        this.phoneNumber = phoneNumber;
        this.socket = socket;
        this.writer = writer;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}