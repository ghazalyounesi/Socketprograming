package com.messaging.server.models;

import lombok.Getter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

@Getter
public class HostConnection {
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;

    public HostConnection(Socket socket, PrintWriter writer, BufferedReader reader) {
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
    }
}
