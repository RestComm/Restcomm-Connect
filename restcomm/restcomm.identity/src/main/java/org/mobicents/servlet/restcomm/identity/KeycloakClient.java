package org.mobicents.servlet.restcomm.identity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

/**
 * General purpose keycloak http client. It retrieves an access token for an application
 * and provides a base class for using keycloak admin REST api.
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class KeycloakClient {
    protected Logger logger = Logger.getLogger(KeycloakClient.class);

    enum Method  {
        GET,
        POST,
        PUT
    }

    static final String DEFAULT_KEYCLOAK_BASE_URL = "https://identity.restcomm.com";
    // Some static configuration options. We should move them to a configuration file at some point.
    String realm; // name of the keycloak realm to use
    String username; // "admin";
    String password; // "password";
    String keycloakBaseUrl;
    String clientApplication; // Name of the keycloak client application to grant access to. for example 'restcomm-rvd-ui' etc.
    List<NameValuePair> params = new ArrayList<NameValuePair>();

    private AccessTokenResponse cachedToken;
    //private HttpServletRequest request; // Store the HTTP request for cleaner API. Remember, KeycloakClient lifecycle follows endpoint lifecycle.

    /*public KeycloakClient(HttpServletRequest request) {
        super();
        if (request == null)
            throw new RuntimeException("Assertion failed. request is null.");
        this.request = request;
    }*/

    public KeycloakClient() {
        this(null,null,null,null,null);
    }

    public KeycloakClient(String username, String password, String clientApplication, String realm,  String keycloakBaseUrl ) {
        super();
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.clientApplication = clientApplication;
        applyConfiguration();
        // and finally validate
        if ( StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isEmpty(clientApplication) || StringUtils.isEmpty(realm) || StringUtils.isEmpty(keycloakBaseUrl) )
            throw new IllegalStateException("Bad parameters while initializing KeycloakClient");
    }

    // initializes client object from configuration taking into account existing values
    private void applyConfiguration() {
        // TODO - load values from restcomm configuration
        if (StringUtils.isEmpty(realm))
            realm = "restcomm"; // load from conf here
        if (StringUtils.isEmpty(username))
            username = "admin"; // load from conf here
        if (StringUtils.isEmpty(password))
            password = "password"; // load from conf here
        if (StringUtils.isEmpty(keycloakBaseUrl))
            keycloakBaseUrl = DEFAULT_KEYCLOAK_BASE_URL; // load from conf here
    }

    // wrap httpclient creation cause it get's complicated when self-signed certs are to be accepted
    public CloseableHttpClient buildHttpClient() throws KeycloakClientException {
        // TODO - use a proper certificate on identity.restcomm.com instead of a self-signed one
        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslsf;
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(),SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new KeycloakClientException(e);
        }
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        return httpclient;
    }

    // Retrieves an access token from keycloak and caches it
    public AccessTokenResponse getToken() throws KeycloakClientException {
        // first check the token cache
        if ( cachedToken != null )
            return cachedToken;

        CloseableHttpClient client = buildHttpClient();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getBaseUrl() + "/auth")
                    .path(ServiceUrlConstants.TOKEN_PATH).build(realm));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientApplication));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                throw new KeycloakClientOauthException(status);
            }
            if (entity == null) {
                throw new KeycloakClientOauthException(status);
            }
            String json = getContent(entity);
            // store the new token in the cache too for future uses (in the same request)
            cachedToken = JsonSerialization.readValue(json, AccessTokenResponse.class);
            return cachedToken;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void makePostRequest(String url) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        CloseableHttpClient client = buildHttpClient();
        MediaType mediaType = MediaType.APPLICATION_FORM_URLENCODED_TYPE; // by default
        try {
            HttpPost request = new HttpPost(url);
            request.addHeader("Authorization", "Bearer " + res.getToken());
            if ( mediaType == MediaType.APPLICATION_JSON_TYPE )
                request.addHeader("Content-Type",MediaType.APPLICATION_JSON);
            if ( ! params.isEmpty() )
                request.setEntity(new UrlEncodedFormEntity(params));
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }  catch (UnsupportedEncodingException e1) {
            throw new KeycloakClientException();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addParam(String name, String value) {
        params.add(new BasicNameValuePair(name,value));
    }


    public void logout() throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        //HttpClient client = new DefaultHttpClient();
        //HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        CloseableHttpClient client = buildHttpClient();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getBaseUrl() + "/auth")
                    .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .build(realm));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, res.getRefreshToken()));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientApplication));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = client.execute(post);
            boolean status = response.getStatusLine().getStatusCode() != 204;
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return;
            }
            InputStream is = entity.getContent();
            if (is != null) is.close();
            if (status) {
                throw new RuntimeException("failed to logout");
            }
        } catch (IOException e) {
            throw new KeycloakClientException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    // Returns the URL origin where keycloak lies. First tries from a configuration value that is explicitly defined. It falls back to geting the value from the request assuming that keycloak runs side by side with restcomm.
    public String getBaseUrl() {
        return keycloakBaseUrl;
        /*
        if ( keycloakBaseUrl != null && !keycloakBaseUrl.equals("") )
            return keycloakBaseUrl;

        String useHostname = request.getServletContext().getInitParameter("useHostname");
        if (useHostname != null && "true".equalsIgnoreCase(useHostname)) {
            return "http://" + HostUtils.getHostName() + ":8080";
        } else {
            return UriUtils.getOrigin(request.getRequestURL().toString());
        }*/
    }

    public static String getContent(HttpEntity entity) throws IOException {
        if (entity == null) return null;
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String data = new String(bytes);
            return data;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    public static class KeycloakClientException extends Exception {
        private Integer httpStatusCode;
        public KeycloakClientException() {}
        public KeycloakClientException(Integer status) {
            this.httpStatusCode = status;
        }
        public KeycloakClientException(Throwable cause) {
            super(cause);
        }
        public Integer getHttpStatusCode() {
            return httpStatusCode;
        }
        public boolean isHttpError() {
            return (httpStatusCode != null);
        }
    }

    // throw when part of the Oauth negotiation with keycloak fails
    public static class KeycloakClientOauthException extends KeycloakClientException {

        public KeycloakClientOauthException() {
            super();
            // TODO Auto-generated constructor stub
        }

        public KeycloakClientOauthException(Integer status) {
            super(status);
            // TODO Auto-generated constructor stub
        }
    }

    public static class KeycloakUserNotFound extends Exception {

        public KeycloakUserNotFound(String message) {
            super(message);
        }
    }
}
