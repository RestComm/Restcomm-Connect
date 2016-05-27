package org.mobicents.servlet.restcomm.rvd.identity;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides accounts by either quereing Restcomm or accessing cache. It follows the application lifecycle (not creatd per-request)
 * Future support for account caching will be added.
 *
 * The class has been implemented as a singleton and is lazily created because it's not possible to initialize it
 * in RvdInitializationServlet (restcommBaseUrl is not available at that time) :-(.
 *
 * @author Orestis Tsakiridis
 */
public class AccountProvider {

    String restcommUrl;

    AccountProvider(String restcommUrl) {
        if (restcommUrl == null)
            throw new IllegalStateException("restcommUrl cannot be null");
        this.restcommUrl = sanitizeRestcommUrl(restcommUrl);
    }

    private String sanitizeRestcommUrl(String restcommUrl) {
        restcommUrl = restcommUrl.trim();
        if (restcommUrl.endsWith("/"))
            return restcommUrl.substring(0,restcommUrl.length()-1);
        return restcommUrl;
    }

    private URI buildAccountQueryUrl(String username) {
        try {
            // TODO url-encode the username
            URI uri = new URIBuilder(restcommUrl).setPath("/restcomm/2012-04-24/Accounts.json/" + username).build();
            return uri;
        } catch (URISyntaxException e) {
            // something really wrong has happened
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the account for username using authorization header as credentials. It can handle both basic http and bearer auth auth.
     * If the authentication fails or the account is not found it returns null.
     * TODO we need to treat differently missing accounts and failed authentications.
     *
     * @param authorizationHeader
     * @return
     */
    public RestcommAccountInfoResponse getAccount(String username, String authorizationHeader) {
        CloseableHttpClient client = CustomHttpClientBuilder.buildHttpClient();
        HttpGet GETRequest = new HttpGet(buildAccountQueryUrl(username));
        GETRequest.addHeader("Authorization", authorizationHeader);
        try {
            CloseableHttpResponse response = client.execute(GETRequest);
            if (response.getStatusLine().getStatusCode() == 200 ) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String accountJson = EntityUtils.toString(entity);
                    Gson gson = new Gson();
                    RestcommAccountInfoResponse accountResponse = gson.fromJson(accountJson, RestcommAccountInfoResponse.class);
                    return accountResponse;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public RestcommAccountInfoResponse getAccount(BasicAuthCredentials creds) {
        String header = "Basic " + RvdUtils.buildHttpAuthorizationToken(creds.getUsername(),creds.getPassword());
        return getAccount(creds.getUsername(), header);
    }

    // singleton stuff
    private static AccountProvider instance;
    public static AccountProvider getInstance() {
        if (instance == null) {
            String restcommUrl = RvdConfiguration.getInstance().getRestcommBaseUri().toString();
            instance = new AccountProvider(restcommUrl);
        }
        return instance;
    }

}

