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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class PlayCollect {
    private final List<URI> initialPrompts;
    private final boolean clearDigitBuffer;
    private final int maxNumberOfDigits;
    private final int minNumberOfDigits;
    private final String digitPattern;
    private final long firstDigitTimer;
    private final long interDigitTimer;
    private final String endInputKey;

    private PlayCollect(final List<URI> initialPrompts, final boolean clearDigitBuffer, final int maxNumberOfDigits,
            final int minNumberOfDigits, final String digitPattern, final long firstDigitTimer, final long interDigitTimer,
            final String endInputKey) {
        super();
        this.initialPrompts = initialPrompts;
        this.clearDigitBuffer = clearDigitBuffer;
        this.maxNumberOfDigits = maxNumberOfDigits;
        this.minNumberOfDigits = minNumberOfDigits;
        this.digitPattern = digitPattern;
        this.firstDigitTimer = firstDigitTimer;
        this.interDigitTimer = interDigitTimer;
        this.endInputKey = endInputKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<URI> initialPrompts() {
        return initialPrompts;
    }

    public boolean clearDigitBuffer() {
        return clearDigitBuffer;
    }

    public int maxNumberOfDigits() {
        return maxNumberOfDigits;
    }

    public int minNumberOfDigits() {
        return minNumberOfDigits;
    }

    public String digitPattern() {
        return digitPattern;
    }

    public long firstDigitTimer() {
        return firstDigitTimer;
    }

    public long interDigitTimer() {
        return interDigitTimer;
    }

    public String endInputKey() {
        return endInputKey;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        if (!initialPrompts.isEmpty()) {
            buffer.append("ip=");
            for (int index = 0; index < initialPrompts.size(); index++) {
                buffer.append(initialPrompts.get(index));
                if (index < initialPrompts.size() - 1) {
                    buffer.append(";");
                }
            }
        }
        if (clearDigitBuffer) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("cb=").append("true");
        }
        if (maxNumberOfDigits > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("mx=").append(maxNumberOfDigits);
        }
        if (minNumberOfDigits > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("mn=").append(minNumberOfDigits);
        }
        if (digitPattern != null) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("dp=").append(digitPattern);
        }
        if (firstDigitTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("fdt=").append(firstDigitTimer * 10);
        }
        if (interDigitTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("idt=").append(interDigitTimer * 10);
        }
        if (endInputKey != null) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("eik=").append(endInputKey);
        }
        return buffer.toString();
    }

    public static final class Builder {
        private List<URI> initialPrompts;
        private boolean clearDigitBuffer;
        private int maxNumberOfDigits;
        private int minNumberOfDigits;
        private String digitPattern;
        private long firstDigitTimer;
        private long interDigitTimer;
        private String endInputKey;

        private Builder() {
            super();
            initialPrompts = new ArrayList<URI>();
            clearDigitBuffer = false;
            maxNumberOfDigits = -1;
            minNumberOfDigits = -1;
            digitPattern = null;
            firstDigitTimer = -1;
            interDigitTimer = -1;
            endInputKey = null;
        }

        public PlayCollect build() {
            return new PlayCollect(initialPrompts, clearDigitBuffer, maxNumberOfDigits, minNumberOfDigits, digitPattern,
                    firstDigitTimer, interDigitTimer, endInputKey);
        }

        public void addPrompt(final URI prompt) {
            this.initialPrompts.add(prompt);
        }

        public void setClearDigitBuffer(final boolean clearDigitBuffer) {
            this.clearDigitBuffer = clearDigitBuffer;
        }

        public void setMaxNumberOfDigits(final int maxNumberOfDigits) {
            this.maxNumberOfDigits = maxNumberOfDigits;
        }

        public void setMinNumberOfDigits(final int minNumberOfDigits) {
            this.minNumberOfDigits = minNumberOfDigits;
        }

        public void setDigitPattern(final String digitPattern) {
            this.digitPattern = digitPattern;
        }

        public void setFirstDigitTimer(final long firstDigitTimer) {
            this.firstDigitTimer = firstDigitTimer;
        }

        public void setInterDigitTimer(final long interDigitTimer) {
            this.interDigitTimer = interDigitTimer;
        }

        public void setEndInputKey(final String endInputKey) {
            this.endInputKey = endInputKey;
        }
    }
}
