package org.mobicents.servlet.restcomm.rvd.upgrade;

import org.apache.log4j.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProjectUpgrader714To10 extends ProjectUpgrader {
    static final Logger logger = Logger.getLogger(ProjectUpgrader714To10.class.getName());

    public ProjectUpgrader714To10() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Stub function to upgrade directly from json string instead of JsonElement
     */
    public JsonElement upgrade(String source) {
        JsonParser parser = new JsonParser();
        JsonElement sourceRoot = parser.parse(source);


        return this.upgrade(sourceRoot);
    }

    /**
     * Upgrades a ProjectState JsonElement to the next version in the version path
     */
    public JsonElement upgrade(JsonElement sourceElement) {

        logger.info("Upgrading project from version rvd714 to 1.0");

        JsonObject source = sourceElement.getAsJsonObject();
        JsonObject target = new JsonObject();

        return target;
    }

    @Override
    public String getResultingVersion() {
        return "1.0";
    }

}
