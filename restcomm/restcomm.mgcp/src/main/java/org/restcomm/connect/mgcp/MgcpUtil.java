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

package org.restcomm.connect.mgcp;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import org.apache.commons.lang.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gdubina on 23.06.17.
 */
public final class MgcpUtil {

    public static final int RETURNCODE_PARTIAL = 101;

    private MgcpUtil(){}

    public static Map<String, String> parseParameters(final String input) {
        final Map<String, String> parameters = new HashMap<String, String>();
        final String[] tokens = input.split(" ");
        for (final String token : tokens) {
            final String[] values = token.split("=");
            if (values.length == 1) {
                parameters.put(values[0], null);
            } else if (values.length == 2) {
                parameters.put(values[0], values[1]);
            }
        }
        return parameters;
    }

    public static boolean isPartialNotify(EventName lastEvent){
        final MgcpEvent event = lastEvent.getEventIdentifier();
        final Map<String, String> parameters = MgcpUtil.parseParameters(event.getParms());
        return NumberUtils.toInt(parameters.get("rc")) == RETURNCODE_PARTIAL;
    }
}
