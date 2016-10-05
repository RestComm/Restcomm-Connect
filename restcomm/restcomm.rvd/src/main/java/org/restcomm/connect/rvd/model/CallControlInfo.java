package org.restcomm.connect.rvd.model;

import java.util.List;

public class CallControlInfo {

    /*public class ApiServer {
        public String username;
        public String pass;
    }*/
    public class Lane {
        public StartPoint startPoint;
        public List<InterruptPoint> interruptPoints;
    }
    public class StartPoint {
        public String rcmlUrl;
        public String to;
        public String from;
    }
    public class InterruptPoint {
        public String code;
    }
    //public class EndPoint {}

    //public ApiServer apiServer;
    public List<Lane> lanes;
    public String accessToken;
}
