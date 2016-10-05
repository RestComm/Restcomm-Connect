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
import org.junit.Rule;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.RvdConfigurationBuilder;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.restcomm.connect.rvd.configuration.RestcommConfigBuilder;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProviderBuilder;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;


/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RestServiceMockedTest {

    ServletContext servletContext;
    HttpServletRequest request;
    RvdConfiguration configuration;
    UserIdentityContext userIdentityContext;
    AccountProvider accountProvider;

    public void setupMocks() {
        // mock HttpServletRequest object
        request = Mockito.mock(HttpServletRequest.class);
        String authorizationHeader = "Basic YWRtaW5pc3RyYXRvckBjb21wYW55LmNvbTo3N2Y4YzEyY2M3YjhmODQyM2U1YzM4YjAzNTI0OTE2Ng=="; // any password will pass
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        // RvdConfiguration
        configuration = new RvdConfigurationBuilder()
                .setRestcommBaseUri("http://127.0.0.1:8099")
                .setRestcommConfig(new RestcommConfigBuilder().build())
                .build(); // point that to wiremock
        CustomHttpClientBuilder httpClientBuilder = new CustomHttpClientBuilder(configuration);
        accountProvider = new AccountProviderBuilder()
                .setRestcommUrl(configuration.getRestcommBaseUri().toString())
                .setHttpClientbuilder(httpClientBuilder)
                .build();
        // mock UserIdentityContext that is required for creating SecuredEndpoint
        userIdentityContext = new UserIdentityContext(authorizationHeader,accountProvider);
    }

    protected void addLegitimateAccount(String email, String accountSid) {
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/" + email))
//                .withHeader("Accept", equalTo("text/xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"" + accountSid + "\",\"email_address\":\"" + email + "\",\"status\":\"active\",\"role\":\"administrator\"}")));
    }


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8099);
}
