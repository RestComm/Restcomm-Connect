/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.servlet.restcomm.telephony;

/**
 * Created by gvagenas on 18/03/16.
 */
public enum HangupReason {
    NO_ANSWER("Call_No_Answer"),
    BUSY("Call_Busy"),
    FAILED("Call_Failed"),
    CANCELED("Call_Canceled"),
    TIMEOUT("Call_Timeout"),
    INVALID("Invalid"),
    NORMAL_CLEARING("Normal_Clearing"),
    LCM_HANGUP_CANCELED("LCM_canceled"),
    LCM_HANGUP_COMPLETED("LCM_completed"),
    LCM_HANGUP_NORMAL_CLEARING("LCM_normal_clearing"),
    PARSER_EXCEPTION("Exception_to_parse_downloaded_RCML"),
    UNDEFINED("Undefined");

    private final String description;
    public String getDescription() {return description; }
    HangupReason(String description) { this.description = description; }
}
