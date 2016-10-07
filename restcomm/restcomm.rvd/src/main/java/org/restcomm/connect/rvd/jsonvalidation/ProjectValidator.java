package org.restcomm.connect.rvd.jsonvalidation;

import java.io.IOException;

import org.restcomm.connect.rvd.jsonvalidation.exceptions.ValidationFrameworkException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import org.restcomm.connect.rvd.RvdConfiguration;

public class ProjectValidator implements Validator {

    String schemaVersion;
    JsonSchema projectSchema;

    public ProjectValidator() throws ProcessingException, IOException {
        init( RvdConfiguration.getRvdProjectVersion() );
    }

    public ProjectValidator(  /*, String uri,*/ String schemaVersion ) throws ProcessingException, IOException {
        init(schemaVersion);
    }

    private void init(String schemaVersion) throws ProcessingException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        projectSchema = factory.getJsonSchema("resource:/validation/rvdproject/" + schemaVersion + "/rvdproject-schema.json#/rvdproject");
    }

    @Override
    public ValidationResult validate(String json) throws ValidationFrameworkException {
        JsonNode state;
        try {
            state = JsonLoader.fromString(json);
            ProcessingReport report;
            report = projectSchema.validate(state);
            //System.out.println(report);
            return new ValidationResult(report);
        } catch (IOException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        } catch (ProcessingException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        }
    }



}
