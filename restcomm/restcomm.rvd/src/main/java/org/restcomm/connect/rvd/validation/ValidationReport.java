package org.restcomm.connect.rvd.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidationReport {

    // nested validation report items
    public static class ValidationErrorItem {
        public ValidationErrorItem(String message, String path) {
            this.message = message;
            this.path = path;
        }
        String message;
        String path;
    }

    public ValidationReport() {
        ok = true; // assume everything model is validated. Change when addErrorItem() is first run
    }

    private List<ValidationErrorItem> errorItems = new ArrayList<ValidationErrorItem>();
    private boolean ok;

    public boolean isOk() {
        return ok;
    }

    public void addErrorItem(String message, String path) {
        errorItems.add(new ValidationErrorItem(message, path));
        ok = false;
    }

    public void addErrorItem(String message) {
        addErrorItem(message, null);
    }
}
