package org.restcomm.connect.commons.telephony;


/**
 * Created by gvagenas on 26/06/2017.
 */
public class ProxyRule {
    private final String fromUri;
    private final String toUri;
    private final String username;
    private final String password;
    private final String patchSdpUri;

    public ProxyRule (final String fromUri, final String toUri, final String username, final String password, final String patchSdpUri) {
        this.fromUri = fromUri;
        this.toUri = toUri;
        this.username = username;
        this.password = password;
        this.patchSdpUri = patchSdpUri;
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

    public String getPatchSdpUri () {
        return patchSdpUri;
    }

    @Override
    public String toString () {
        String msg = String.format("Proxy rule-> FromUri %s | ToUri %s | Username %s | Password: %s | PatchSDP %s", fromUri, toUri, username, password, patchSdpUri);
        return msg;
    }
}
