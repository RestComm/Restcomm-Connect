package org.mobicents.servlet.restcomm.rvd.http.resources;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.RvdConfigurationBuilder;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.mobicents.servlet.restcomm.rvd.configuration.RestcommConfigBuilder;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProviderBuilder;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;


/**
 * Created by nando on 10/4/16.
 */
public class RestServiceMockedTest {

    ServletContext servletContext;
    HttpServletRequest request;
    RvdConfiguration configuration;
    UserIdentityContext userIdentityContext;

    @Before
    public void before() {
        // setup wiremock server to simulate authentication
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts.json/administrator@company.com"))
//                .withHeader("Accept", equalTo("text/xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"email_address\":\"administrator@company.com\",\"status\":\"active\",\"role\":\"administrator\"}")));
        // mock HttpServletRequest object
        request = Mockito.mock(HttpServletRequest.class);
        String authorizationHeader = "Basic YWRtaW5pc3RyYXRvckBjb21wYW55LmNvbTo3N2Y4YzEyY2M3YjhmODQyM2U1YzM4YjAzNTI0OTE2Ng==";
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        // RvdConfiguration
        configuration = new RvdConfigurationBuilder()
                .setRestcommBaseUri("http://127.0.0.1:8089")
                .setRestcommConfig(new RestcommConfigBuilder().build())
                .build(); // point that to wiremock
        CustomHttpClientBuilder httpClientBuilder = new CustomHttpClientBuilder(configuration);
        AccountProvider accountProvider = new AccountProviderBuilder()
                .setRestcommUrl(configuration.getRestcommBaseUri().toString())
                .setHttpClientbuilder(httpClientBuilder)
                .build();
        // mock UserIdentityContext that is required for creating SecuredEndpoint
        userIdentityContext = new UserIdentityContext(authorizationHeader,accountProvider);
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

}
