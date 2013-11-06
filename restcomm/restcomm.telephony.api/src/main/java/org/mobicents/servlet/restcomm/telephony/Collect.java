/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.telephony;

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
