package org.mobicents.servlet.restcomm.rvd;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RvdUtils {
    public static boolean isEmpty( String value) {
        if ( value == null || "".equals(value) )
            return true;
        return false;
    }

    /**
     * Reduces Map<String,String[]> HttpServletRequest.getParameters() map multivalue parameters to
     * single value ones. It does this by keeping only the first array item.
     * @param requestMap
     * @return The parsed Map<String, String>
     */
    public static Map<String,String> reduceHttpRequestParameterMap(Map<String,String[]> requestMap) {
        Map<String,String> reducedMap = new HashMap<String,String>();
        for ( Entry<String,String[]> entry : requestMap.entrySet()) {
            reducedMap.put(entry.getKey(), entry.getValue()[0]); // parameter arrays should have at least one value
        }
        return reducedMap;
    }
}
