package org.mobicents.servlet.restcomm.rvd.upgrade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public abstract class  ProjectUpgrader {

    public JsonElement upgrade(String source) {
        JsonParser parser = new JsonParser();
        JsonElement sourceRoot = parser.parse(source);

        return this.upgrade(sourceRoot);
    }

    public abstract JsonElement upgrade(JsonElement sourceElement);

    public abstract String getResultingVersion();
}
