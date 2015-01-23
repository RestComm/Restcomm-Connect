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

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * Use this to notify a Call object that needs to Record
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class StartRecordingCall {

    public enum RecordingType {
        DO_NOT_RECORD("do-not-record"), RECORD_FROM_ANSWER("record-from-answer"), RECORD_FROM_RINGING("record-from-ringing");

        private final String text;

        private RecordingType(final String text) {
            this.text = text;
        }

        public static RecordingType getValueOf(final String text) {
            RecordingType[] values = values();
            for (final RecordingType value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid account status.");
        }

        @Override
        public String toString() {
            return text;
        }
    };

    private Sid callId;
    private Sid accountId;
    private Configuration runtimeSetting;
    private DaoManager daoManager;
    private Sid recordingSid;
    private URI recordingUri;

    public StartRecordingCall(final Sid accountId, final Configuration runtimeSettings, final DaoManager daoManager,
            final Sid recordingSid, final URI recordingUri) {
        this.accountId = accountId;
        this.runtimeSetting = runtimeSettings;
        this.daoManager = daoManager;
        this.recordingSid = recordingSid;
        this.recordingUri = recordingUri;
    }

    public Sid getAccountId() {
        return accountId;
    }

    public Configuration getRuntimeSetting() {
        return runtimeSetting;
    }

    public DaoManager getDaoManager() {
        return daoManager;
    }

    public Sid getRecordingSid() {
        return recordingSid;
    }

    public URI getRecordingUri() {
        return recordingUri;
    }

    public void setCallId(Sid callId) {
        this.callId = callId;
    }

    public Sid getCallId() {
        return callId;
    }

}
