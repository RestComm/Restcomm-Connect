package org.mobicents.servlet.restcomm.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

@Immutable
public class CallDetailRecordFilter {

    private final String accountSid;
    private final String recipient;
    private final String sender;
    private final String status;
    private final Date startTime;  // to initialize it pass string arguments with  yyyy-MM-dd format
    private final String parentCallSid;
    private final Integer limit;
    private final Integer offset;

    public CallDetailRecordFilter(String accountSid, String recipient, String sender, String status, String startTime,
            String parentCallSid, Integer limit, Integer offset) throws ParseException {
        this.accountSid = accountSid;

        // The LIKE keyword uses '%' to match any (including 0) number of characters, and '_' to match exactly one character
        // Add here the '%' keyword so +15126002188 will be the same as 15126002188 and 6002188
        if (recipient != null)
            recipient = "%".concat(recipient);
        if (sender != null)
            sender = "%".concat(sender);

        this.recipient = recipient;
        this.sender = sender;
        this.status = status;
        this.parentCallSid = parentCallSid;
        this.limit = limit;
        this.offset = offset;
        if (startTime != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(startTime);
            this.startTime = date;
        } else
            this.startTime = null;
    }

    public String getSid() {
        return accountSid;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSender() {
        return sender;
    }

    public String getStatus() {
        return status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String getParentCallSid() {
        return parentCallSid;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

}
