package org.restcomm.connect.rvd.model.steps.dial;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class RcmlDialStep extends RcmlStep {
    List<RcmlNoun> nouns = new ArrayList<RcmlNoun>();
    String action;
    String method;
    String timeout;
    String timeLimit;
    String callerId;
    Boolean record;
}
