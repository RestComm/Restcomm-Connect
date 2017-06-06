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
package org.restcomm.connect.sms.api;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.extension.api.IExtensionCreateSmsSessionRequest;
/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class CreateSmsSession implements IExtensionCreateSmsSessionRequest {
    private final String from;
    private final String to;
    private final String accountSid;
    private final boolean isFromApi;
    private Configuration configuration;
    private boolean allowed = true;


    //This will be used to create SmsSession from
    // 1. REST API SmsMessageEndpoint - Send SMS from REST API
    // 2. BaseVoiceInterpreter.CreatingSmsSession - Send SMS using "SMS" RCML verb in voice application
    // 3. SmsInterpreter.CreatingSmsSession - Send SMS using "SMS" RCML verb in sms application
    // 4. SmppInterpreter.CreatingSmsSession - Send SMS using "SMS" RCML verb when SMPP link is activated
    public CreateSmsSession(final String from, final String to, final String accountSid, final boolean isFromApi) {
        this.from = from;
        this.to = to;
        this.accountSid = accountSid;
        this.isFromApi = isFromApi;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isFromApi() {
        return isFromApi;
    }

    @Override
    public String toString() {
        return "From: "+from+" , To: "+to+" , AccountSid: "+accountSid+" , isFromApi: "+isFromApi;
    }

    /**
     * IExtensionRequest
     * @return accountSid
     */
    @Override
    public String getAccountSid() {
        return accountSid;
    }

    /**
     * IExtensionRequest
     * @return if allowed
     */
    @Override
    public boolean isAllowed() {
        return this.allowed;
    }

    /**
     * IExtensionRequest
     * @param set allowed
     */
    @Override
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    /**
     * IExtensionCreateSmsSessionRequest
     * @param set Configuration object
     */
    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * IExtensionCreateSmsSessionRequest
     * @return Configuration object
     */
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }
}
