package org.mobicents.servlet.restcomm.rvd.validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

public class ValidationResult {
    public static class ErrorItem {
        String level;
        String failurePath;
        String summary;
    }

    private ProcessingReport report;
    private boolean success;
    private List<ErrorItem> errorItems = new ArrayList<ErrorItem>();


    public ValidationResult(ProcessingReport report) {
        this.report = report;

        this.success = report.isSuccess();
        Iterator<ProcessingMessage> i = report.iterator();
        // !! Checks only the first message. Maybe there are error that produce more messages
        while (i.hasNext()) {

            JsonNode node = i.next().asJson();
            ErrorItem item = new ErrorItem();
            item.level = node.get("level").asText();
            item.failurePath = node.get("instance").get("pointer").asText();
            item.summary = node.get("message").asText();
            //System.out.println(node.get("level").asText() );
            //System.out.println(node.get("instance").get("pointer").asText());
            //System.out.println(node.get("message").asText());
            //System.out.println(node.toString() + "\n\n");
            errorItems.add(item);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ErrorItem> getErrorItems() {
        return errorItems;
    }

    public ProcessingReport getReport() {
        return report;
    }

}
