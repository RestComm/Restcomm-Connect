package org.mobicents.servlet.restcomm.rvd;

public class RvdUtils {
    public static boolean isEmpty( String value) {
        if ( value == null || "".equals(value) )
            return true;
        return false;
    }
}
