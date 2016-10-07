package org.restcomm.connect.rvd.identity;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.restcomm.connect.rvd.commons.GenericResponse;
import org.restcomm.connect.rvd.commons.http.CustomHttpClientBuilder;
import org.restcomm.connect.rvd.restcomm.RestcommAccountInfo;
import org.restcomm.connect.rvd.utils.RvdUtils;

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
    CustomHttpClientBuilder httpClientBuilder;


    public AccountProvider(String restcommUrl, CustomHttpClientBuilder httpClientBuilder) {
        if (restcommUrl == null)
            throw new IllegalStateException("restcommUrl cannot be null");
        this.restcommUrl = sanitizeRestcommUrl(restcommUrl);
        this.httpClientBuilder = httpClientBuilder;
    }

    private String sanitizeRestcommUrl(String restcommUrl) {
        restcommUrl = restcommUrl.trim();
        if (restcommUrl.endsWith("/"))
            return restcommUrl.substring(0,restcommUrl.length()-1);
        return restcommUrl;
    }

    private URI buildAccountQueryUrl(String usernameOrSid) {
        try {
            // TODO url-encode the username
            URI uri = new URIBuilder(restcommUrl).setPath("/restcomm/2012-04-24/Accounts.json/" + usernameOrSid).build();
            return uri;
        } catch (URISyntaxException e) {
            // something really wrong has happened
            throw new RuntimeException(e);
        }
    }


    public GenericResponse<RestcommAccountInfo> getAccount(String authorizationHeader, String accountName) {
        CloseableHttpClient client = httpClientBuilder.buildHttpClient();
        HttpGet GETRequest = new HttpGet(buildAccountQueryUrl(accountName));
        GETRequest.addHeader("Authorization", authorizationHeader);
        try {
            CloseableHttpResponse response = client.execute(GETRequest);
            if (response.getStatusLine().getStatusCode() == 200 ) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String accountJson = EntityUtils.toString(entity);
                    Gson gson = new Gson();
                    RestcommAccountInfo accountResponse = gson.fromJson(accountJson, RestcommAccountInfo.class);
                    if ("active".equals(accountResponse.getStatus()))
                        return new GenericResponse<>(accountResponse);
                }
            } else
                return new GenericResponse<>(response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new GenericResponse<>("Something went wrong while retrieving account " + accountName);
    }

    /**
     * Retrieves account 'accountName' from restcomm using creds as credentials.
     * If the authentication fails or the account is not found it returns null.
     *
     * TODO we need to treat differently missing accounts and failed authentications.
     *
     */
    public GenericResponse<RestcommAccountInfo> getAccount(BasicAuthCredentials creds, String accountName) {
        String header = "Basic " + RvdUtils.buildHttpAuthorizationToken(creds.getUsername(),creds.getPassword());
        return getAccount(header, accountName);
    }

    public GenericResponse<RestcommAccountInfo> getAccount(BasicAuthCredentials creds) {
        String header = "Basic " + RvdUtils.buildHttpAuthorizationToken(creds.getUsername(),creds.getPassword());
        return getAccount(header, creds.getUsername());
    }
}

