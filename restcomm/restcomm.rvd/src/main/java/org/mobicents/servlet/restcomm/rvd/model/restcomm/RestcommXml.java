package org.mobicents.servlet.restcomm.rvd.model.restcomm;

public class RestcommXml {

    public Restcomm restcomm;

    public static class Restcomm {
        public RuntimeSettings runtimeSettings;
    }
    public static class RuntimeSettings {
        public String recordingsUri;
        //String promptsUri;
    }
}
