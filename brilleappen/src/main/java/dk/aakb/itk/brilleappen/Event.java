package dk.aakb.itk.brilleappen;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Event {
    public final String title;
    public final String addFileUrl;
    public final List<ContactPerson> contactPeople;

    Event(String json) {
        Map values = Util.getValues(json);
        this.title = (String) Util.getDrupalValue(values, "title");
        this.addFileUrl = (String) values.get("add_file_url");
        this.contactPeople = new ArrayList<>();

        try {
            List<LinkedTreeMap> list = (List) values.get("field_gg_contact_people");
            for (LinkedTreeMap item : list) {
                this.contactPeople.add(new ContactPerson(item));
            }
        } catch (Exception e) {
        }
    }
}
