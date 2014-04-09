/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * Use this to notify a Call object that needs to Record
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

    private Sid accountId;
    private Configuration runtimeSetting;
    private DaoManager daoManager;

    public StartRecordingCall(final Sid accountId, final Configuration runtimeSettings, final DaoManager daoManager) {
        this.accountId = accountId;
        this.runtimeSetting = runtimeSettings;
        this.daoManager = daoManager;
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
}
