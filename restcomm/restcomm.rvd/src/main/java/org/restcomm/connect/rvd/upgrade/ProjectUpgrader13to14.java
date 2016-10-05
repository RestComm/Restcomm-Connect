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

/**
 * @author Orestis Tsakiridis
 */
public class ProjectUpgrader13to14 implements ProjectUpgrader {
    @Override
    public JsonElement upgrade(JsonElement sourceElement) {
        // only version fields needs upgrade from 1.3 to 1.4
        return ProjectUpgrader10to11.setVersion(sourceElement, getResultingVersion());
    }

    @Override
    public String getResultingVersion() {
        return "1.4";
    }
}
