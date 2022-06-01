package de.vzg.service.wordpress.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Optional;

public class FailSafeAuthorsDeserializer implements JsonDeserializer<MayAuthorList> {

    @Override
    public MayAuthorList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        MayAuthorList mayAuthorList = new MayAuthorList();

        if(json.isJsonArray()){
            final JsonArray asJsonArray = json.getAsJsonArray();

            asJsonArray.forEach(el-> {
                if(el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    Optional.ofNullable(obj.get("display_name"))
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .ifPresent(mayAuthorList.getAuthorNames()::add);
                } else if(el.isJsonPrimitive()) {
                    mayAuthorList.getAuthorIds().add(el.getAsInt());
                }
            });

        }


        return mayAuthorList;
    }
}
