/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.rvd.model;

import java.util.UUID;
import java.util.regex.Pattern;


public final class ProjectSid {
    public static final Pattern pattern = Pattern.compile("[a-zA-Z0-9]{34}");
    private final String id;

    public ProjectSid(final String id) throws IllegalArgumentException {
        super();
        if (pattern.matcher(id).matches()) {
            this.id = id;
        } else {
            throw new IllegalArgumentException(id + " is an INVALID_SID sid value.");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        final ProjectSid other = (ProjectSid) object;
        if (!toString().equals(other.toString())) {
            return false;
        }
        return true;
    }

    public static ProjectSid generate() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        return new ProjectSid("PR" + uuid);
    }

    @Override
    public int hashCode() {
        final int prime = 5;
        int result = 1;
        result = prime * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return id;
    }
}
