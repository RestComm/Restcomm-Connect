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
import org.mobicents.servlet.restcomm.telephony.CreateCall.RecordingType;

/**
 * Use this to notify a Call object that needs to Record
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class RecordCall {

    private Sid accountId;
    private RecordingType recordingType;
    private Configuration runtimeSetting;
    private DaoManager daoManager;
    private Boolean useItAsFlag;

    public RecordCall(final Sid accountId, final RecordingType recordingType, final Configuration runtimeSettings, final DaoManager daoManager) {
        this.accountId = accountId;
        this.recordingType = recordingType;
        this.runtimeSetting = runtimeSettings;
        this.daoManager = daoManager;
    }

    public RecordCall(final Boolean flag) {
        this.setUseItAsFlag(flag);
    }

    public Sid getAccountId() {
        return accountId;
    }

    public RecordingType getRecordingType() {
        return recordingType;
    }

    public Configuration getRuntimeSetting() {
        return runtimeSetting;
    }

    public DaoManager getDaoManager() {
        return daoManager;
    }

    public Boolean getUseItAsFlag() {
        return useItAsFlag;
    }

    public void setUseItAsFlag(Boolean useItAsFlag) {
        this.useItAsFlag = useItAsFlag;
    }
}
