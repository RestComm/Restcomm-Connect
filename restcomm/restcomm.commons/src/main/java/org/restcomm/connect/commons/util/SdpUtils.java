package org.restcomm.connect.commons.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

//import ThreadSafe;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
//@ThreadSafe
public class SdpUtils {

    /**
     * Patches an SDP description by trimming and making sure it ends with a new line.
     *
     * @param sdpDescription The SDP description to be patched.
     * @return The patched SDP description
     * @author hrosa
     */
    public static String endWithNewLine(String sdpDescription) {
        if (sdpDescription == null || sdpDescription.isEmpty()) {
            throw new IllegalArgumentException("The SDP description cannot be null or empty");
        }
        return sdpDescription.trim().concat("\n");
    }

    @SuppressWarnings("unchecked")
    public static String patch(final String contentType, final byte[] data, final String externalIp)
            throws UnknownHostException, SdpException {
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

    public static String getSdp(final String contentType, final byte[] data) throws SdpParseException {
        final String text = new String(data);
        String sdpResult = null;
        if (contentType.equalsIgnoreCase("application/sdp")) {
            final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(text);
            sdpResult = sdp.toString();
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
            sdpResult = sdp.toString();
        }
        return sdpResult;
    }

    private static void fix(final Origin origin, final String externalIp) throws UnknownHostException, SdpException {
        if (origin != null) {
            if (Connection.IN.equals(origin.getNetworkType())) {
                if (Connection.IP4.equals(origin.getAddressType())) {
                    final InetAddress address;
                    try {
                        address = InetAddress.getByName(origin.getAddress());
                        final String ip = address.getHostAddress();
                        if (!IPUtils.isRoutableAddress(ip)) {
                            origin.setAddress(externalIp);
                        }
                    } catch (UnknownHostException e) {
                        // TODO do nothing cause domain name is unknown
                        // origin.setAddress(externalIp); to remove
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

    public static boolean isWebRTCSDP(final String contentType, final byte[] data) throws SdpParseException {
        boolean isWebRTC = false;
        if (contentType.equalsIgnoreCase("application/sdp")) {
            String sdp = getSdp(contentType, data);
            if (sdp != null && sdp.contains("RTP/SAVP") || sdp.contains("rtp/savp")
                    || sdp.contains("RTP/SAVPF") || sdp.contains("rtp/savpf")) {
                isWebRTC = true;
            }
        }
        return isWebRTC;
    }

}
