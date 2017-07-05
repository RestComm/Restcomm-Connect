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
package org.restcomm.connect.extension.api;

public class ExtensionRequest implements IExtensionRequest{
    private boolean allowed = true;
    private String accountSid;
    public ExtensionRequest() {
        this("", true);
    }
    public ExtensionRequest(String accountSid, boolean allowed) {
        this.accountSid = accountSid;
        this.allowed = allowed;
    }

    /**
     * IExtensionRequest
     * @return get accountSid
     */
    @Override
    public String getAccountSid() {
        return this.accountSid;
    }

    /**
     * IExtensionRequest
     * @return is allowed
     */
    @Override
    public boolean isAllowed() {
        return this.allowed;
    }

    /**
     * IExtensionRequest
     * @param is allowed
     */
    @Override
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

}
