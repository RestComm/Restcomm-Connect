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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Orestis Tsakiridis
 */
public class ProjectUpgrader15to16 implements ProjectUpgrader {
    @Override
    public JsonElement upgrade(JsonElement sourceElement) {
        sourceElement = upgradeNumericCollectMenu(sourceElement);
        sourceElement = ProjectUpgrader10to11.setVersion(sourceElement, getResultingVersion());
        return sourceElement;
    }

    // converts numeric collect-menu keys to strings
    public static JsonElement upgradeNumericCollectMenu(JsonElement rootElement) {
        JsonElement nodesElement = rootElement.getAsJsonObject().get("nodes");
        if ( nodesElement != null) {
            JsonArray nodes = nodesElement.getAsJsonArray();
            for ( int i = 0; i < nodes.size(); i++ ) {
                JsonObject node = nodes.get(i).getAsJsonObject();
                String kind = node.get("kind").getAsString();
                if ( "voice".equals(kind) ) {
                    JsonElement stepsElement = node.get("steps");
                    if ( stepsElement != null ) {
                        JsonArray steps = stepsElement.getAsJsonArray();
                        for ( int steps_i = 0; steps_i < steps.size(); steps_i ++ ) {
                            JsonObject step = steps.get(steps_i).getAsJsonObject();
                            String stepkind = step.get("kind").getAsString();
                            if ( "gather".equals(stepkind) ) {
                                JsonElement menuElement = step.get("menu");
                                if ( menuElement != null ) {
                                    JsonElement mappingsElement = menuElement.getAsJsonObject().get("mappings");
                                    if ( mappingsElement != null) {
                                        JsonArray mappings = mappingsElement.getAsJsonArray();
                                        for ( int mappings_i = 0; mappings_i < mappings.size();  mappings_i ++ ) {
                                            JsonObject mapping = mappings.get(mappings_i).getAsJsonObject();
                                            // convert 'digits' from integer to string
                                            Integer digits = mapping.get("digits").getAsInt();
                                            mapping.remove("digits");
                                            mapping.addProperty("digits", digits.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // handle other project kinds here (sms,ussd)
            }
        }
        return rootElement;
    }

    @Override
    public String getResultingVersion() {
        return "1.6";
    }
}
