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

package org.restcomm.connect.rvd.http.resources;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sun.jersey.core.util.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.restcomm.connect.rvd.ApplicationContext;
import org.restcomm.connect.rvd.ApplicationContextBuilder;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.RvdConfigurationBuilder;
import org.restcomm.connect.rvd.commons.http.CustomHttpClientBuilder;
import org.restcomm.connect.rvd.configuration.RestcommConfigBuilder;
import org.restcomm.connect.rvd.identity.AccountProvider;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.client.ProjectState;

import javax.servlet.ServletContext;

import java.io.File;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RestServiceMockedTest {

    ServletContext servletContext;
    RvdConfiguration configuration;
    AccountProvider accountProvider;
    File workspaceDir;
    ModelMarshaler marshaler;
    ApplicationContext appContext;

    public void setupMocks() {
        configuration = new RvdConfigurationBuilder()
                .setRestcommBaseUri("http://127.0.0.1:8099")
                .setRestcommConfig(new RestcommConfigBuilder().build())
                .build(); // point that to wiremock
        CustomHttpClientBuilder httpClientBuilder = new CustomHttpClientBuilder(configuration);
        accountProvider = new AccountProvider(configuration, httpClientBuilder);
        appContext = new ApplicationContextBuilder().setAccountProvider(accountProvider).setConfiguration(configuration).setHttpClientBuilder(httpClientBuilder).build();
    }

    protected void addLegitimateAccount(String email, String accountSid) {
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + email))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"" + accountSid + "\",\"email_address\":\"" + email + "\",\"status\":\"active\",\"role\":\"administrator\"}")));
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + accountSid))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"" + accountSid + "\",\"email_address\":\"" + email + "\",\"status\":\"active\",\"role\":\"administrator\"}")));

    }

    protected void addMissingAccount(String email, String accountSid) {
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + email)).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + accountSid)).willReturn(aResponse().withStatus(404)));
    }

    protected void addForbiddenAccount(String email, String accountSid) {
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + email)).willReturn(aResponse().withStatus(403)));
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + accountSid)).willReturn(aResponse().withStatus(403)));
    }

    protected void createProject(String projectName, String owner) throws IOException {
        new File(workspaceDir.getPath() + "/" + projectName).mkdir();
        String state = marshaler.toData(ProjectState.createEmptyVoice(owner));
        FileUtils.writeStringToFile(new File(workspaceDir.getPath() + "/" + projectName + "/state"), state );
    }

    protected boolean projectExists(String projectName) {
        File projectDir = new File(workspaceDir.getPath() + "/" + projectName);
        return projectDir.exists();
    }

    protected UserIdentityContext signIn(String username, String authToken) {
        String authorizationHeader = "Basic " + new String(Base64.encode(username + ":" + authToken));
        return new UserIdentityContext(authorizationHeader, accountProvider);
    }



    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8099);
}
