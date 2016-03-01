package dk.aakb.itk.brilleappen;

import java.util.Map;

public class Media {
    public final String id;
    public final String notifyUrl;

    Media(String json) {
        Map<String, Object> values = Util.getValues(json);
        id = (String)values.get("media_id");
        notifyUrl = (String)values.get("notify_url");
    }
}
