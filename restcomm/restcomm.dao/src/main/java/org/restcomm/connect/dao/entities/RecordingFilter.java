/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.connect.dao.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author parallels
 */
public class RecordingFilter {

    private final String accountSid;
    private final List<String> accountSidSet; // if not-null we need the cdrs that belong to several accounts
    private final Date startTime;  // to initialize it pass string arguments with  yyyy-MM-dd format
    private final Date endTime;
    private final String callSid;
    private final Integer limit;
    private final Integer offset;
    private final String instanceid;

    public RecordingFilter(String accountSid, List<String> accountSidSet, String startTime, String endTime,
                                  String callSid, Integer limit, Integer offset) throws ParseException {
        this(accountSid, accountSidSet, startTime,endTime, callSid, limit,offset,null);
    }

    public RecordingFilter(String accountSid, List<String> accountSidSet, String startTime, String endTime,
                                  String callSid, Integer limit, Integer offset, String instanceId) throws ParseException {
        this.accountSid = accountSid;
        this.accountSidSet = accountSidSet;

        this.callSid = callSid;
        this.limit = limit;
        this.offset = offset;
        if (startTime != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(startTime);
            this.startTime = date;
        } else
            this.startTime = null;

        if (endTime != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(endTime);
            this.endTime = date;
        } else {
            this.endTime = null;
        }
        if (instanceId != null && !instanceId.isEmpty()) {
            this.instanceid = instanceId;
        } else {
            this.instanceid = null;
        }
    }

    public String getSid() {
        return accountSid;
    }

    public List<String> getAccountSidSet() {
        return accountSidSet;
    }

    public String getCallSid() {
        return callSid;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getInstanceid() { return instanceid; }

}
