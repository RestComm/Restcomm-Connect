package org.restcomm.connect.commons.amazonS3;

/**
 * Created by gvagenas on 14/03/2017.
 */
public enum RecordingSecurityLevel {
    NONE("none"),REDIRECT("redirect"),SECURE("secure");

    private final String text;

    private RecordingSecurityLevel(final String text) {
        this.text = text;
    }
}
