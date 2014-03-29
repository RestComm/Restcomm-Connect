package org.mobicents.servlet.restcomm.rvd.validation;

import java.io.IOException;

import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationFrameworkException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class ProjectValidator {

    String schemaVersion;
    JsonSchema projectSchema;

    public ProjectValidator(  /*, String uri, String schemaVersion*/ ) throws ProcessingException, IOException {

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        projectSchema = factory.getJsonSchema("resource:/validation/rvdproject-0.1-schema.json#/rvdproject");


        //final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        //JsonNode schemaNode = JsonLoader.fromFile(new File("rvdproject-0.1-schema.json"));
        //final JsonSchema schema = factory.getJsonSchema(schemaNode);

        //JsonNode state = JsonLoader.fromFile(new File("state1"));
        //ProcessingReport report;
        //report = schema.validate(state);
        //System.out.println(report);

        //state = JsonLoader.fromFile(new File("/home/nando/bin/apache-tomcat-6.0.37/webapps/restcomm-rvd/workspace/test/state"));
        //report = schema.validate(state);
        //System.out.println(report);

    }

    public ValidationResult validate(String json) throws ValidationFrameworkException {
        JsonNode state;
        try {
            state = JsonLoader.fromString(json);
            ProcessingReport report;
            report = projectSchema.validate(state);
            System.out.println(report);
            return new ValidationResult(report);
        } catch (IOException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        } catch (ProcessingException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        }
    }



}
