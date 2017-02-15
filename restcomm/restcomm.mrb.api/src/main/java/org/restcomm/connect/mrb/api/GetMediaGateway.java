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

package org.restcomm.connect.mrb.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author Maria Farooq (maria.farooq@telestax.com)
 */
@Immutable
public final class GetMediaGateway {
    private final Sid callSid;
    private final String conferenceName;
    private final String msId;

    /**
     * @param callSid - restcomm callSid - if MediaGateway is required for a call
     * @param conferenceName - friendly name of conference - if MediaGateway is required for a conference
     * @param msId - mediaserver id - provide in case we need MediaGateway of a specific mediaserver in cluster. 
     */
    public GetMediaGateway(final Sid callSid, final String conferenceName, final String msId) {
        super();
        this.callSid = callSid;
        this.conferenceName = conferenceName;
        this.msId = msId;
    }

    /**
     * @param callSid - restcomm callSid - if MediaGateway is required for a call
     */
    public GetMediaGateway(final Sid callSid){
        this(callSid, null, null);
    }

    /**
     * @param msId - mediaserver id - provide in case we need MediaGateway of a specific mediaserver in cluster.
     */
    public GetMediaGateway(final String msId) {
        this(null, null, msId);
    }

    public Sid callSid() {
        return callSid;
    }

    public String conferenceName() {
        return conferenceName;
    }

    public String msId(){
        return msId;
    }

}
