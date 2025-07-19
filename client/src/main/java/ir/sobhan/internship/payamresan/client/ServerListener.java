package ir.sobhan.internship.payamresan.client;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;

@Slf4j
public class ServerListener implements Runnable {

    private final BufferedReader reader;

    public ServerListener(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        try {
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                System.out.println("\n<-- " + serverMessage);
                System.out.print("> ");
            }
        } catch (IOException e) {

            log.info("Connection to server closed. Listener thread is stopping.");
        }
    }
}
