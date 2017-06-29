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
