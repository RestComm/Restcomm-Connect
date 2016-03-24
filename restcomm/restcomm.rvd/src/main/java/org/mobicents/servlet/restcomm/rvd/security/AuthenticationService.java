package org.mobicents.servlet.restcomm.rvd.security;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.RvdSecurityException;

public class AuthenticationService {
    static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());
    String authenticationToken;
    String baseRestcommUrl;

    public AuthenticationService() {
        this.baseRestcommUrl = RvdConfiguration.getInstance().getRestcommBaseUri().toString();
    }

    /**
     * Authenticates a username against restcomm. If successfull, the account is returned. Otherwise
     * null is returned.
     *
     * @param username
     * @param password
     * @return A RestcommAccountInfoResponse object or null
     * @throws RvdSecurityException
     */
    public RestcommAccountInfoResponse authenticate(String username, String password) throws RvdSecurityException {
        logger.debug("Authenticating " + username + " on Restcomm");
        String restcommAuthUrl = baseRestcommUrl;

        CloseableHttpClient client = CustomHttpClientBuilder.buildHttpClient();
        URI url;
        try {
            URIBuilder uriBuilder = new URIBuilder(restcommAuthUrl);
            uriBuilder.setPath("/restcomm/2012-04-24/Accounts.json/" + username);
            url = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RvdSecurityException("Error building restcomm authentication url: " + restcommAuthUrl, e);
        }

        // Set a header or HTTP authentication (looks easier than the apache client way)
        HttpGet get = new HttpGet(url);
        byte[] usernamePassBytes = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
        authenticationToken = Base64.encodeBase64String(usernamePassBytes);
        get.addHeader("Authorization", "Basic " + authenticationToken);

        CloseableHttpResponse response;
        try {
            response = client.execute(get);
        } catch (ClientProtocolException e) {
            throw new RvdSecurityException("Error authenticating on restcomm", e);
        } catch (IOException e) {
            throw new RvdSecurityException("Error authenticating on restcomm", e);
        }

        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(response.getEntity().getContent());
            } catch (IOException e) {
                throw new RvdSecurityException("Error authenticating on restcomm. Authenticated account seems invalid.", e);
            }
            Gson gson = new Gson();
            RestcommAccountInfoResponse accountInfo = gson.fromJson(reader, RestcommAccountInfoResponse.class);
            if (accountInfo.getStatus().equalsIgnoreCase("active")) {
                return accountInfo;
            }
        }
        return null;
    }

    public String getAuthenticationToken() {
        return this.authenticationToken;
    }

}
