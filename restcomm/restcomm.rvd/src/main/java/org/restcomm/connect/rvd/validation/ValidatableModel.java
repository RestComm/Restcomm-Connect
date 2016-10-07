package org.restcomm.connect.rvd.validation;

/**
 * Ancestor class that adds validation capabilities to all extending models
 * @author "Tsakiridis Orestis"
 *
 */
public abstract class ValidatableModel {

    public ValidationReport validate() {
        ValidationReport report = new ValidationReport();
        return validate(report);
    }

    public abstract ValidationReport validate(ValidationReport report);

}
