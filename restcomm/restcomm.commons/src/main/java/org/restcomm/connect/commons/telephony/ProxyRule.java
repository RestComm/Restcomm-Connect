package org.restcomm.connect.commons.telephony;


/**
 * Created by gvagenas on 26/06/2017.
 */
public class ProxyRule {
    private final String fromUri;
    private final String toUri;
    private final String username;
    private final String password;

    public ProxyRule (final String fromUri, final String toUri, final String username, final String password) {
        this.fromUri = fromUri;
        this.toUri = toUri;
        this.username = username;
        this.password = password;
    }

    public String getFromUri () {
        return fromUri;
    }

    public String getToUri () {
        return toUri;
    }

    public String getPassword () {
        return password;
    }

    public String getUsername () {
        return username;
    }
}
