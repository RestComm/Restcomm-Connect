package org.restcomm.connect.monitoringservice;

import org.restcomm.connect.telephony.api.CallInfo;

import java.util.List;

/**
 * Created by gvagenas on 15/05/2017.
 */
public class LiveCallsDetails {

    private final List<CallInfo> callDetails;

    public LiveCallsDetails (List<CallInfo> callDetails) {
        this.callDetails = callDetails;
    }

    public List<CallInfo> getCallDetails () {
        return callDetails;
    }
}
