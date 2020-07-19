package io.github.xf8b.adminbot.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class ConfigUtil {
    private static JsonObject readConfig(File file) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonReader jsonReader;
        try {
            jsonReader = gson.newJsonReader(new FileReader(file));
        } catch (FileNotFoundException exception) {
            throw new IllegalStateException("The config file does not exist!");
        }
        JsonElement jsonElement = JsonParser.parseReader(jsonReader);
        return jsonElement.getAsJsonObject();
    }

    public static String readToken(File file) {
        JsonObject jsonObject = readConfig(file);
        if (jsonObject.get("token") == null) {
            throw new IllegalStateException("Token does not exist in config!");
        }
        return jsonObject.get("token").getAsString();
    }

    public static String readActivity(File file) {
        JsonObject jsonObject = readConfig(file);
        if (jsonObject.get("activity") == null) {
            throw new IllegalStateException("Activity does not exist in config!");
        }
        return jsonObject.get("activity").getAsString();
    }

}
