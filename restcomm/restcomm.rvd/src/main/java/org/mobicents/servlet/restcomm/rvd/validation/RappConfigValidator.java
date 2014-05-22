package org.mobicents.servlet.restcomm.rvd.validation;

import java.io.IOException;

import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationFrameworkException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonSchema;

public class RappConfigValidator implements Validator {

    //String schemaVersion;
    JsonSchema schema;

    public RappConfigValidator() throws RvdException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        String path = "resource:/validation/packaging/" + RvdSettings.getRvdProjectVersion() + "/rappConfig";
        try {
            schema = factory.getJsonSchema(path);
        } catch (ProcessingException e) {
            throw new RvdException("Error processing schema " + path, e);
        }
    }

    @Override
    public ValidationResult validate(String json) throws ValidationFrameworkException {
        JsonNode jsonnode;
        try {
            jsonnode = JsonLoader.fromString(json);
            ProcessingReport report;
            report = schema.validate(jsonnode);
            return new ValidationResult(report);
        } catch (IOException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        } catch (ProcessingException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        }
    }

}
