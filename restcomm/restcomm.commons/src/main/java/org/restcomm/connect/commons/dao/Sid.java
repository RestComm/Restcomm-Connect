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
import org.restcomm.connect.commons.configuration.RestcommConfiguration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author maria-farooq@live.com (Maria Farooq)
 */
@Immutable
public final class Sid {

    private static String CALL_SID_STRING = "ID[a-zA-Z0-9]{32}-CA[a-zA-Z0-9]{32}";
    public static final Pattern pattern = Pattern.compile("[a-zA-Z0-9]{34}");
    public static final Pattern callSidPattern = Pattern.compile(CALL_SID_STRING);

    private final String id;

    public enum Type {
        CALL(null, CALL_SID_STRING),
        ACCOUNT("AC"),
        APPLICATION("AP"),
        ANNOUNCEMENT("AN"),

        CLIENT("CL"),
        CONFERENCE("CF"),
        GATEWAY("GW"),

        NOTIFICATION("NO"),
        PHONE_NUMBER("PN"),
        RECORDING("RE"),
        REGISTRATION("RG"),
        SHORT_CODE("SC"),
        SMS_MESSAGE("SM"),
        TRANSCRIPTION("TR"),
        INSTANCE("ID"),
        EXTENSION_CONFIGURATION("EX"),
        GEOLOCATION("GL"),
        ORGANIZATION("OR"),
        PROFILE("PR"),
        INVALID("IN");

        private final String prefix;
        private final String regex;
        private final Pattern pattern;
        private static final String UUID_PATTERN = "[a-zA-Z0-9]{32}";
        private Type(final String prefix) {
            this(prefix, null);
        }
        private Type(final String prefix, String regex) {
            this.prefix = prefix;
            if(regex==null) {
                this.regex = prefix + UUID_PATTERN;
            }else{
                this.regex = regex;
            }
            pattern = Pattern.compile(this.regex);
        }
        public String getPrefix() {
            return prefix;
        }
        public boolean isType(Sid sid) {
            return pattern.matcher(sid.toString()).matches();
        }
    };

    private static final Sid INVALID_SID = new Sid("IN00000000000000000000000000000000");

    public Sid(final String id) throws IllegalArgumentException {
        super();
        //https://github.com/RestComm/Restcomm-Connect/issues/1907
        if (callSidPattern.matcher(id).matches() || pattern.matcher(id).matches()) {
            this.id = id;
        } else {
            throw new IllegalArgumentException(id + " is an INVALID_SID sid value.");
        }
    }

    public static Type getType(Sid sid) {
        Type res = Type.INVALID;

        for(Type type: Type.values()) {
            if(type.isType(sid)) {
                res = type;
                break;
            }
        }
        return res;
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
                return new Sid(type.getPrefix() + token);
            }
            default: {
                return generate(type);
            }
        }
    }

    public static Sid generate(final Type type) {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        switch (type) {
            case CALL: {
                //https://github.com/RestComm/Restcomm-Connect/issues/1907
                return new Sid(RestcommConfiguration.getInstance().getMain().getInstanceId() + "-CA" + uuid);
            }
            case INVALID:{
                return INVALID_SID;
            }
            case ACCOUNT:
            case APPLICATION:
            case ANNOUNCEMENT:
            case CLIENT:
            case CONFERENCE:
            case GATEWAY:
            case NOTIFICATION:
            case PHONE_NUMBER:
            case RECORDING:
            case REGISTRATION:
            case SHORT_CODE:
            case SMS_MESSAGE:
            case TRANSCRIPTION:
            case INSTANCE:
            case EXTENSION_CONFIGURATION:
            case GEOLOCATION:
            case ORGANIZATION:
            case PROFILE:{
                return new Sid(type.getPrefix() + uuid);
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
