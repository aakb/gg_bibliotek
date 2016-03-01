package dk.aakb.itk.brilleappen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

abstract class Util {
    static Map<String, Object> getValues(String json) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    static String toJson(Object value) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(value);
    }
}
