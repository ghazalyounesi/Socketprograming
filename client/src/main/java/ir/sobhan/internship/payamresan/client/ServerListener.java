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
            // در یک حلقه بی‌نهایت، منتظر پیام از سرور می‌مانیم
            while ((serverMessage = reader.readLine()) != null) {
                // با دریافت هر پیام، آن را در کنسول چاپ می‌کنیم
                System.out.println("\n<-- " + serverMessage);
                System.out.print("> "); // دوباره علامت > را برای ورودی کاربر چاپ می‌کنیم
            }
        } catch (IOException e) {
            // این خطا زمانی رخ می‌دهد که اتصال بسته شود (مثلاً بعد از disconnect)
            // این یک رفتار طبیعی است و نیازی به لاگ کردن خطا نیست.
            log.info("Connection to server closed. Listener thread is stopping.");
        }
    }
}
