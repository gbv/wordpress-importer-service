package de.vzg.wis.configuration;

import java.lang.reflect.Type;
import java.util.Optional;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BackwardsCompatibleLicenseDeserializer implements JsonDeserializer<ImporterConfigurationLicense> {
    @Override
    public ImporterConfigurationLicense deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {

        ImporterConfigurationLicense license = new ImporterConfigurationLicense();

        if(jsonElement.isJsonPrimitive()){
            String licenseStr = jsonElement.getAsString();

            license.setClassID(licenseStr);
            license.setLogoURL("https://i.creativecommons.org/l/" + licenseStr + "/80x15.png");
            license.setURL("https://creativecommons.org/licenses/" + licenseStr);

            switch (licenseStr) {
                case "by-nc-nd/4.0":
                    license.setClassID("cc_by-nc-nd_4.0");
                    break;
                case "by-nc-sa/4.0":
                    license.setClassID("cc_by-nc-sa_4.0");
                    break;
                case "by-nc/4.0":
                    license.setClassID("cc_by-nc_4.0");
                    break;
                case "by-nd/4.0":
                    license.setClassID("cc_by-nd_4.0");
                    break;
                case "by-sa/4.0":
                    license.setClassID("cc_by-sa_4.0");
                    break;
                case "by/4.0":
                    license.setClassID("cc_by_4.0");
                    break;
            }
        } else if (jsonElement.isJsonObject()) {
            JsonObject licenseObj = jsonElement.getAsJsonObject();

            Optional.ofNullable(licenseObj.get("classID")).map(JsonElement::getAsString).ifPresent(license::setClassID);
            Optional.ofNullable(licenseObj.get("URL")).map(JsonElement::getAsString).ifPresent(license::setURL);
            Optional.ofNullable(licenseObj.get("logoURL")).map(JsonElement::getAsString).ifPresent(license::setLogoURL);
            Optional.ofNullable(licenseObj.get("label")).map(JsonElement::getAsString).ifPresent(license::setLabel);

        } else {
            throw new JsonParseException("Expected str or obj for license");
        }

        return license;
    }
}
