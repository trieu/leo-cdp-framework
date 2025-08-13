package test.revenue_test;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimestampDeserializer implements JsonDeserializer<Timestamp> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new Timestamp(dateFormat.parse(json.getAsString()).getTime());
        } catch (ParseException e) {
            throw new JsonParseException("Failed parsing '" + json.getAsString() + "' as Timestamp");
        }
    }
}
