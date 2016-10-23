package org.restcomm.connect.rvd.identity;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

public class SecurityUtils {

    /**
     * Extracts username and password from a Basic HTTP "Authorization" header. Expects only the value
     * of the header. Thus, for header "Authorization: xyx" it expects only the "xyz" part.
     *
     * @param headerValue
     * @return a BasicAuthCredentials object or null if no credentials are found or a parsing error occurs
     */
    public static BasicAuthCredentials parseBasicAuthHeader(String headerValue) {
        if (headerValue != null) {
            String[] parts = headerValue.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2) {
                    BasicAuthCredentials credentialsObj = new BasicAuthCredentials(values[0], values[1]);
                    return credentialsObj;
                }

            }
        }
        return null;
    }

}
