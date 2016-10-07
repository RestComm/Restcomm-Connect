package org.restcomm.connect.rvd.model.steps.ussdcollect;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;
import org.restcomm.connect.rvd.model.steps.ussdsay.UssdSayRcml;

public class UssdCollectRcml extends RcmlStep {

    String action;
    List<UssdSayRcml> messages = new ArrayList<UssdSayRcml>();

    public UssdCollectRcml() {
        // TODO Auto-generated constructor stub
    }

}
