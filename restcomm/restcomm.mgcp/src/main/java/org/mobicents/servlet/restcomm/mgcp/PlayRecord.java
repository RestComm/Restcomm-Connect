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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class PlayRecord {
    private final List<URI> initialPrompts;
    private final boolean clearDigitBuffer;
    private final long preSpeechTimer;
    private final long postSpeechTimer;
    private final long recordingLength;
    private final String endInputKey;
    private final URI recordingId;

    private PlayRecord(final List<URI> initialPrompts, final boolean clearDigitBuffer, final long preSpeechTimer,
            final long postSpeechTimer, final long recordingLength, final String endInputKey, final URI recordingId) {
        super();
        this.initialPrompts = initialPrompts;
        this.clearDigitBuffer = clearDigitBuffer;
        this.preSpeechTimer = preSpeechTimer;
        this.postSpeechTimer = postSpeechTimer;
        this.recordingLength = recordingLength;
        this.endInputKey = endInputKey;
        this.recordingId = recordingId;
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

    public long preSpeechTimer() {
        return preSpeechTimer;
    }

    public long postSpeechTimer() {
        return postSpeechTimer;
    }

    public long recordingLength() {
        return recordingLength;
    }

    public String endInputKey() {
        return endInputKey;
    }

    public URI recordingId() {
        return recordingId;
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
        if (recordingId != null) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("ri=").append(recordingId);
        }
        if (clearDigitBuffer) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("cb=").append("true");
        }
        if (preSpeechTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("prt=").append(preSpeechTimer * 10);
        }
        if (postSpeechTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("pst=").append(postSpeechTimer * 10);
        }
        if (recordingLength > 0) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("rlt=").append(recordingLength * 1000);
        }
        if (endInputKey != null) {
            if (buffer.length() > 0)
                buffer.append(" ");
            buffer.append("eik=").append(endInputKey).append(" ");
            buffer.append("mn=").append(endInputKey.length()).append(" ");
            buffer.append("mx=").append(endInputKey.length());
        }
        return buffer.toString();
    }

    public static final class Builder {
        private List<URI> initialPrompts;
        private boolean clearDigitBuffer;
        private long preSpeechTimer;
        private long postSpeechTimer;
        private long recordingLength;
        private String endInputKey;
        private URI recordingId;

        private Builder() {
            super();
            initialPrompts = new ArrayList<URI>();
            clearDigitBuffer = false;
            preSpeechTimer = -1;
            postSpeechTimer = -1;
            recordingLength = -1;
            endInputKey = null;
            recordingId = null;
        }

        public PlayRecord build() {
            return new PlayRecord(initialPrompts, clearDigitBuffer, preSpeechTimer, postSpeechTimer, recordingLength,
                    endInputKey, recordingId);
        }

        public void addPrompt(final URI prompt) {
            this.initialPrompts.add(prompt);
        }

        public void setClearDigitBuffer(final boolean clearDigitBuffer) {
            this.clearDigitBuffer = clearDigitBuffer;
        }

        public void setPreSpeechTimer(final long preSpeechTimer) {
            this.preSpeechTimer = preSpeechTimer;
        }

        public void setPostSpeechTimer(final long postSpeechTimer) {
            this.postSpeechTimer = postSpeechTimer;
        }

        public void setRecordingLength(final long recordingLength) {
            this.recordingLength = recordingLength;
        }

        public void setEndInputKey(final String endInputKey) {
            this.endInputKey = endInputKey;
        }

        public void setRecordingId(final URI recordingId) {
            this.recordingId = recordingId;
        }
    }
}
