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
package org.mobicents.servlet.restcomm.mscontrol.messages;

import java.net.URI;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Collect {
    private final List<URI> prompts;
    private final String pattern;
    private final int timeout;
    private final String endInputKey;
    private final int numberOfDigits;

    public Collect(final List<URI> prompts, final String pattern, final int timeout, final String endInputKey,
            final int numberOfDigits) {
        super();
        this.prompts = prompts;
        this.pattern = pattern;
        this.timeout = timeout;
        this.endInputKey = endInputKey;
        this.numberOfDigits = numberOfDigits;
    }

    public List<URI> prompts() {
        return prompts;
    }

    public String pattern() {
        return pattern;
    }

    public int timeout() {
        return timeout;
    }

    public String endInputKey() {
        return endInputKey;
    }

    public int numberOfDigits() {
        return numberOfDigits;
    }
}
