package de.vzg.service.wordpress.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class FailSafeAuthorsDeserializer implements JsonDeserializer<MayAuthorList> {

    @Override
    public MayAuthorList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if(!json.isJsonArray()){
            return new MayAuthorList();
        }
        final JsonArray asJsonArray = json.getAsJsonArray();
        final ArrayList<Integer> integers = new ArrayList<>(asJsonArray.size());
        asJsonArray.forEach(el-> {
            final int integer = el.getAsInt();
            integers.add(integer);
        });
        return new MayAuthorList(integers);
    }
}
