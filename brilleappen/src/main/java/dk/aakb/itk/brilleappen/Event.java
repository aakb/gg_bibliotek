package dk.aakb.itk.brilleappen;

import java.util.List;
import java.util.Map;

public class Event {
    public final String title;
    public final String addFileUrl;
    public final List<ContactPerson> contactPeople;

    Event(String json ) {
        Map<String, Object> values = Util.getValues(json);
        this.title = (String)values.get("title");
        this.addFileUrl = (String)values.get("add_file_url");

        throw new UnsupportedOperationException("Not implemented");
    }
}
