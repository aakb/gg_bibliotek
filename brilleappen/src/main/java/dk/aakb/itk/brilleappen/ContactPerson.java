package dk.aakb.itk.brilleappen;

import java.util.Map;

public class ContactPerson {
    public final String name;
    public final String phone;
    public final String email;

    ContactPerson(String json) {
        Map<String, Object> values = Util.getValues(json);
        this.name = (String)values.get("name");
        this.phone = (String)values.get("phone");
        this.email = (String)values.get("email");
    }
}
