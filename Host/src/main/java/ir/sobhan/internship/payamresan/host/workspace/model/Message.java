package ir.sobhan.internship.payamresan.host.workspace.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

@Getter
@AllArgsConstructor
public class Message {
    private final int seq;
    private final String fromUsername;
    private final String type;
    private final String body;

    /**
     * این متد یک نمایش JSON از پیام برای ارسال به کلاینت‌ها می‌سازد.
     */
    public String toJsonString() {
        JSONObject json = new JSONObject();
        json.put("seq", this.seq);
        json.put("from", this.fromUsername);
        json.put("type", this.type);
        json.put("body", this.body);
        return json.toString();
    }
}


