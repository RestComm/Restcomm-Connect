package org.restcomm.connect.rvd.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.restcomm.connect.rvd.exceptions.RvdException;



public class RvdUtils {

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public RvdUtils() {
        // TODO Auto-generated constructor stub
    }

    public static File createTempDir() throws RvdException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
          File tempDir = new File(baseDir, baseName + counter);
          if (tempDir.mkdir()) {
            return tempDir;
          }
        }
        throw new RvdException("Failed to create directory within "
            + TEMP_DIR_ATTEMPTS + " attempts (tried "
            + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
      }

    public static boolean isEmpty( String value) {
        if ( value == null || "".equals(value) )
            return true;
        return false;
    }

    // returns True when either the value is True OR null
    public static boolean isEmpty( Boolean value) {
        if ( value == null || value == false )
            return true;
        return false;
    }

    public static boolean safeEquals(String value1, String value2) {
        return value1 == null ? (value1 == value2) : (value1.equals(value2));
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

    public static String buildHttpAuthorizationToken(String username, String password) {
        byte[] usernamePassBytes = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
        String authenticationToken = Base64.encodeBase64String(usernamePassBytes);
        return authenticationToken;
    }

    public static String myUrlEncode(String value) {
        try {
            // TODO make sure plus characters in the original value are handled correctly
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            // TODO issue a warning here
            return value;
        }
    }
}
