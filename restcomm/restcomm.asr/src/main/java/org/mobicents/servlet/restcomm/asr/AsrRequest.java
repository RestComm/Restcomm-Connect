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
package org.mobicents.servlet.restcomm.asr;

import java.io.File;
import java.util.Map;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class AsrRequest {
    private final File file;
    private final String language;
    private final Map<String, Object> attributes;

    public AsrRequest(final File file, final String language, final Map<String, Object> attributes) {
        super();
        this.file = file;
        this.language = language;
        this.attributes = attributes;
    }

    public AsrRequest(final File file, final String language) {
        this(file, language, null);
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public File file() {
        return file;
    }

    public String language() {
        return language;
    }
}
