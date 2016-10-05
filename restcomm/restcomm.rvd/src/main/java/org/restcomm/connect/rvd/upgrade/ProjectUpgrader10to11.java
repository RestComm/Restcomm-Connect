/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.rvd.upgrade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Orestis Tsakiridis
 */
public class ProjectUpgrader10to11 implements ProjectUpgrader {
    @Override
    public JsonElement upgrade(JsonElement sourceElement) {
        // only version fields needs upgrade from 1.0 to 1.1
        return setVersion(sourceElement, getResultingVersion());
    }

    /**
     * Set project vesion to newVersion. Assumes "root.header.version" existence introduced
     * in project version 1.0
     *
     * @param root
     * @param newVersion
     * @return the same root JsonElement it was given (for easy chaining)
     */
    public static JsonElement setVersion(JsonElement root, String newVersion) {
        JsonObject header = root.getAsJsonObject().get("header").getAsJsonObject();
        header.remove("version");
        header.addProperty("version",newVersion);
        return root;
    }

    /**
     * Returns project version. Assumes "root.header.version" existence introduced
     * in project version 1.0
     *
     * @param root
     * @return the project version as a string
     */
    public static String getVersion(JsonElement root) {
        JsonObject header = root.getAsJsonObject().get("header").getAsJsonObject();
        return header.get("version").getAsString();
    }

    @Override
    public String getResultingVersion() {
        return "1.1";
    }
}
