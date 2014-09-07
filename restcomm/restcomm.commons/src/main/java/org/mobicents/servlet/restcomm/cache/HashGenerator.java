package org.mobicents.servlet.restcomm.cache;

import org.apache.shiro.crypto.hash.Sha256Hash;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class HashGenerator {

    public static String hashMessage(String first, String second, String third) {
        return new Sha256Hash(first + second + third).toHex();
    }

}
