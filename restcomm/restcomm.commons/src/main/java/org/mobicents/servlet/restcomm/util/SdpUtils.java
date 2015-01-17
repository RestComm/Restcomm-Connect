package org.mobicents.servlet.restcomm.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@ThreadSafe
public class SdpUtils {

    @SuppressWarnings("unchecked")
    public static String patch(final String contentType, final byte[] data, final String externalIp) throws UnknownHostException,
            SdpException {
        final String text = new String(data);
        String patchedSdp = null;
        if (contentType.equalsIgnoreCase("application/sdp")) {
            final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(text);
            // Handle the connection at the session level.
            fix(sdp.getConnection(), externalIp);
            // https://github.com/Mobicents/RestComm/issues/149
            fix(sdp.getOrigin(), externalIp);
            // Handle the connections at the media description level.
            final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
            for (final MediaDescription description : descriptions) {
                fix(description.getConnection(), externalIp);
            }
            patchedSdp = sdp.toString();
        } else {
            String boundary = contentType.split(";")[1].split("=")[1];
            String[] parts = text.split(boundary);
            String sdpText = null;
            for (String part : parts) {
                if (part.contains("application/sdp")) {
                    sdpText = part.replaceAll("Content.*", "").replaceAll("--", "").trim();
                }
            }
            final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(sdpText);
            fix(sdp.getConnection(), externalIp);
            // https://github.com/Mobicents/RestComm/issues/149
            fix(sdp.getOrigin(), externalIp);
            // Handle the connections at the media description level.
            final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
            for (final MediaDescription description : descriptions) {
                fix(description.getConnection(), externalIp);
            }
            patchedSdp = sdp.toString();
        }
        return patchedSdp;
    }

    private static void fix(final Origin origin, final String externalIp) throws UnknownHostException, SdpException {
        if (origin != null) {
            if (Connection.IN.equals(origin.getNetworkType())) {
                if (Connection.IP4.equals(origin.getAddressType())) {
                    final InetAddress address = InetAddress.getByName(origin.getAddress());
                    final String ip = address.getHostAddress();
                    if (!IPUtils.isRoutableAddress(ip)) {
                        origin.setAddress(externalIp);
                    }
                }
            }
        }
    }

    private static void fix(final Connection connection, final String externalIp) throws UnknownHostException, SdpException {
        if (connection != null) {
            if (Connection.IN.equals(connection.getNetworkType())) {
                if (Connection.IP4.equals(connection.getAddressType())) {
                    final InetAddress address = InetAddress.getByName(connection.getAddress());
                    final String ip = address.getHostAddress();
                    if (!IPUtils.isRoutableAddress(ip)) {
                        connection.setAddress(externalIp);
                    }
                }
            }
        }
    }

}
