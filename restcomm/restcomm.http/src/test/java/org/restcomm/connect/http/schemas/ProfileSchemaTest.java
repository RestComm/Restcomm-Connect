package org.restcomm.connect.http.schemas;

import org.junit.Test;

/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.ws.rs.core.Link;

import junit.framework.Assert;
import org.restcomm.connect.commons.dao.Sid;

/**
 *
 * @author
 */
public class ProfileSchemaTest {

    public ProfileSchemaTest() {
    }

    @Test
    public void testFreePlan() throws Exception {
        final JsonNode fstabSchema = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/rc-profile-schema.json");
        final JsonNode good = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/freePlan.json");

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

        final JsonSchema schema = factory.getJsonSchema(fstabSchema);

        ProcessingReport report;

        report = schema.validate(good);
        Assert.assertTrue(report.isSuccess());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        final JsonNode fstabSchema = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/rc-profile-schema.json");
        final JsonNode good = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/emptyProfile.json");

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

        final JsonSchema schema = factory.getJsonSchema(fstabSchema);

        ProcessingReport report;

        report = schema.validate(good);
        Assert.assertTrue(report.isSuccess());
    }

    @Test
    public void testRetrieveAllowedPrefixes() throws Exception {
        final JsonNode good = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/freePlan.json");
        JsonPointer pointer = JsonPointer.compile("/featureEnablement/destinations/allowedPrefixes");
        JsonNode at = good.at(pointer);
        Assert.assertNotNull(at);
        Assert.assertTrue(at.isArray());
        Assert.assertEquals("+1", at.get(0).asText());
    }

    @Test
    public void testInvalidFeature() throws Exception {
        final JsonNode fstabSchema = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/rc-profile-schema.json");
        final JsonNode good = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/invalidFeature.json");

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

        final JsonSchema schema = factory.getJsonSchema(fstabSchema);

        ProcessingReport report;

        report = schema.validate(good);
        Assert.assertFalse(report.isSuccess());
    }

    @Test
    public void testLink() throws Exception {
        String linkheader = "<meta.rdf>;rel=meta";
        Link valueOf = Link.valueOf(linkheader);
        Assert.assertEquals("meta", valueOf.getRel());
        Assert.assertEquals("meta.rdf", valueOf.getUri().toString());

        Sid pSid = Sid.generate(Sid.Type.PROFILE);
        String profileURL = "http://cloud.restcomm.com/profiles/" + pSid;
        Link build = Link.fromUri(profileURL).rel("related").build();
        Assert.assertEquals("<" + profileURL + ">; rel=\"related\"", build.toString());

        Path paths =  Paths.get(build.getUri().getPath());
        Sid targetSid = new Sid (paths.getName(paths.getNameCount() - 1).toString());
    }
}
