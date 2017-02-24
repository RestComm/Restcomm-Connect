package org.restcomm.connect.rvd.jsonvalidation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

public class ValidationResult {

    private boolean success;
    private List<ValidationErrorItem> errorItems = new ArrayList<ValidationErrorItem>();

    public ValidationResult(ProcessingReport report) {
        this.success = report.isSuccess();
        Iterator<ProcessingMessage> i = report.iterator();
        // !! Checks only the first message. Maybe there are error that produce more messages
        while (i.hasNext()) {
            JsonNode node = i.next().asJson();
            ValidationErrorItem item = new ValidationErrorItem(node.get("message").asText(), node.get("level").asText(), node.get("instance").get("pointer").asText() );
            errorItems.add(item);
        }
    }

    public void appendError(ValidationErrorItem error) {
        errorItems.add(error);
        if ("error".equals(error.level))
            this.success = false;
    }

    public boolean isSuccess() {
        return success;
    }

}
