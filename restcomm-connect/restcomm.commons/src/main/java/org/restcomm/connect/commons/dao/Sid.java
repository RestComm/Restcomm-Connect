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
package org.restcomm.connect.commons.dao;

import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Sid {
    public static final Pattern pattern = Pattern.compile("[a-zA-Z0-9]{34}");
    private final String id;

    public enum Type {
        ACCOUNT, APPLICATION, ANNOUNCEMENT, CALL, CLIENT, CONFERENCE, GATEWAY, INVALID, NOTIFICATION, PHONE_NUMBER, RECORDING, REGISTRATION, SHORT_CODE, SMS_MESSAGE, TRANSCRIPTION, INSTANCE, EXTENSION_CONFIGURATION, GEOLOCATION
    };

    private static final Sid INVALID_SID = new Sid("IN00000000000000000000000000000000");

    public Sid(final String id) throws IllegalArgumentException {
        super();
        if (pattern.matcher(id).matches()) {
            this.id = id;
        } else {
            throw new IllegalArgumentException(id + " is an INVALID_SID sid value.");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        final Sid other = (Sid) object;
        if (!toString().equals(other.toString())) {
            return false;
        }
        return true;
    }

    // Issue 108: https://bitbucket.org/telestax/telscale-restcomm/issue/108/account-sid-could-be-a-hash-of-the
    public static Sid generate(final Type type, String string) {
        String token = new Md5Hash(string).toString();
        switch (type) {
            case ACCOUNT: {
                return new Sid("AC" + token);
            }
            default: {
                return generate(type);
            }
        }
    }

    public static Sid generate(final Type type) {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        switch (type) {
            case ACCOUNT: {
                return new Sid("AC" + uuid);
            }
            case APPLICATION: {
                return new Sid("AP" + uuid);
            }
            case ANNOUNCEMENT: {
                return new Sid("AN" + uuid);
            }
            case CALL: {
                return new Sid("CA" + uuid);
            }
            case CLIENT: {
                return new Sid("CL" + uuid);
            }
            case CONFERENCE: {
                return new Sid("CF" + uuid);
            }
            case GATEWAY: {
                return new Sid("GW" + uuid);
            }
            case INVALID: {
                return INVALID_SID;
            }
            case NOTIFICATION: {
                return new Sid("NO" + uuid);
            }
            case PHONE_NUMBER: {
                return new Sid("PN" + uuid);
            }
            case RECORDING: {
                return new Sid("RE" + uuid);
            }
            case REGISTRATION: {
                return new Sid("RG" + uuid);
            }
            case SHORT_CODE: {
                return new Sid("SC" + uuid);
            }
            case SMS_MESSAGE: {
                return new Sid("SM" + uuid);
            }
            case TRANSCRIPTION: {
                return new Sid("TR" + uuid);
            }
            case INSTANCE: {
                return new Sid("ID" + uuid);
            }
            case EXTENSION_CONFIGURATION: {
                return new Sid("EX" + uuid);
            }
            case GEOLOCATION: {
                return new Sid("GL" + uuid);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 5;
        int result = 1;
        result = prime * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return id;
    }
}
