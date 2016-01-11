/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.rvd.restcomm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.rvd.exceptions.AccessApiException;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.model.HttpScheme;
import org.mobicents.servlet.restcomm.rvd.model.UserProfile;
import org.mobicents.servlet.restcomm.rvd.model.WorkspaceSettings;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.Gson;

/**
 * @author Orestis Tsakiridis
 */
public class RestcommClient {

    private final String protocol;
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private boolean authenticationTokenAsPassword = false;
    CloseableHttpClient apacheClient;

    public static class RestcommClientException extends AccessApiException {

        public RestcommClientException(String message, Throwable cause) {
            super(message, cause);
            // TODO Auto-generated constructor stub
        }

        public RestcommClientException(String message) {
            super(message);
            // TODO Auto-generated constructor stub
        }

    }

    public static class RestcommClientInitializationException extends RvdException {
        public RestcommClientInitializationException(String message) {
            super(message);
        }

    }

    public static class Request {
        RestcommClient client;
        String method;
        String path;
        ArrayList<String> paramNames = new ArrayList<String>();
        ArrayList<String> paramValues = new ArrayList<String>();

        public Request(RestcommClient client, String method, String path) {
            this.client = client;
            this.method = method;
            this.path = path;
        }

        public Request addParam(String name, String value) {
            if (!RvdUtils.isEmpty(name)) {
                paramNames.add(name);
                paramValues.add(value);
            }
            return this;
        }

        public Request addParams(HashMap<String, String> params) {
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String name = String.valueOf(pair.getKey());
                String value = String.valueOf(pair.getValue());
                if (!RvdUtils.isEmpty(name) && !RvdUtils.isEmpty(value)) {
                    paramNames.add(name);
                    paramValues.add(value);
                }
                it.remove();
            }
            return this;
        }

        public <T> T done(Gson gson, Class<T> resultClass) throws AccessApiException {
            // Build the uri for the call made to Restcomm
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setHost(client.host);
            uriBuilder.setPort(client.port);
            uriBuilder.setScheme(client.protocol);
            uriBuilder.setPath(path);

            try {

                CloseableHttpResponse apiResponse;
                if ("GET".equals(method)) {
                    for (int i = 0; i < paramNames.size(); i++)
                        uriBuilder.addParameter(paramNames.get(i), paramValues.get(i));
                    String uri = uriBuilder.build().toString();
                    HttpGet get = new HttpGet(uri);
                    get.addHeader("Authorization", "Basic " + getAuthenticationToken());
                    apiResponse = client.apacheClient.execute(get);
                    try {
                        Integer statusCode = apiResponse.getStatusLine().getStatusCode();
                        if (statusCode != 200) {
                            if (statusCode == 401)
                                throw new RestcommClientException("Authentication failed while using Restcomm REST api")
                                        .setStatusCode(statusCode);
                            else
                                throw new RestcommClientException("Error invoking Restcomm REST api").setStatusCode(statusCode);
                        }
                        return gson.fromJson(new InputStreamReader(apiResponse.getEntity().getContent()), resultClass);
                    } finally {
                        apiResponse.close();
                    }
                } else if ("POST".equals(method)) {
                    String uri = uriBuilder.build().toString();
                    HttpPost post = new HttpPost(uri);
                    List<NameValuePair> values = new ArrayList<NameValuePair>();
                    for (int i = 0; i < paramNames.size(); i++) {
                        values.add(new BasicNameValuePair(paramNames.get(i), paramValues.get(i)));
                    }
                    post.setEntity(new UrlEncodedFormEntity(values));
                    post.addHeader("Authorization", "Basic " + getAuthenticationToken());
                    apiResponse = client.apacheClient.execute(post);
                    try {
                        Integer statusCode = apiResponse.getStatusLine().getStatusCode();
                        if (statusCode != 200) {
                            if (statusCode == 401)
                                throw new RestcommClientException("Authentication failed while using Restcomm REST api")
                                        .setStatusCode(statusCode);
                            else
                                throw new RestcommClientException("Error invoking Restcomm REST api").setStatusCode(statusCode);
                        }
                        String content = IOUtils.toString(apiResponse.getEntity().getContent());
                        return gson.fromJson(content, resultClass);
                    } finally {
                        apiResponse.close();
                    }
                } else if ("DELETE".equals(method)) {
                    String uri = uriBuilder.build().toString();
                    HttpDelete delete = new HttpDelete(uri);
                    List<NameValuePair> values = new ArrayList<NameValuePair>();
                    for (int i = 0; i < paramNames.size(); i++) {
                        values.add(new BasicNameValuePair(paramNames.get(i), paramValues.get(i)));
                    }
                    delete.addHeader("Authorization", "Basic " + getAuthenticationToken());
                    apiResponse = client.apacheClient.execute(delete);
                    try {
                        Integer statusCode = apiResponse.getStatusLine().getStatusCode();
                        if (statusCode != 200) {
                            if (statusCode == 401)
                                throw new RestcommClientException("Authentication failed while using Restcomm REST api")
                                        .setStatusCode(statusCode);
                            else
                                throw new RestcommClientException("Error invoking Restcomm REST api").setStatusCode(statusCode);
                        }
                        String content = IOUtils.toString(apiResponse.getEntity().getContent(), Charset.forName("UTF-8"));
                        return gson.fromJson( content, resultClass );
                    } finally {
                        apiResponse.close();
                    }
                } else
                    throw new UnsupportedOperationException("Only GET, POST and DELETE methods are supported");

            } catch (IOException e) {
                throw new RestcommClientException("Error contacting: " + path, e);
            } catch (URISyntaxException e) {
                throw new RestcommClientException("Error building URL from this path: " + path, e);
            }

        }

        private String getAuthenticationToken() {
            if (client.authenticationTokenAsPassword) {
                return client.password;
            } else {
                return RvdUtils.buildHttpAuthorizationToken(client.username, client.password);
            }
        }

    }

    public RestcommClient(String protocol, String host, int port, String username, String password) {
        // TODO Auto-generated constructor stub
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        apacheClient = CustomHttpClientBuilder.buildHttpClient();
    }


    /**
     *   Creates and initializes a RestcommClient combining information from various sources. Try to use this
     *   constructor for contacting restcomm to have logic in a single place.
     *
     *   Algorithm for initializing properties for accessing restcomm REST API, namely host, port,
     *   username and password.
     *
     *   For restcomm host & port:
     *    1. Try using the workspace .settings file. If host,port is found use it.
     *    2. Otherwise, check the user profile.
     *    3. If all this fails, use RestcommBaseUri (connector information)
     *
     *   For user credentials:
     *    1. Use the user profile
     *
     * @param workspaceSettings
     * @param profile
     * @param fallbackRestcommBaseUri
     * @throws RestcommClientInitializationException
     */
    public RestcommClient (WorkspaceSettings workspaceSettings, UserProfile profile, URI fallbackRestcommBaseUri, String usernameOverride, String passwordOverride) throws RestcommClientInitializationException {
        // initialize host
        String apiHost = null;
        if (workspaceSettings != null) {
            if ( ! RvdUtils.isEmpty(workspaceSettings.getApiServerHost()) )
                apiHost = workspaceSettings.getApiServerHost();
        }
        if (apiHost == null) {
            if ( profile != null && ! RvdUtils.isEmpty(profile.getRestcommHost()) )
                apiHost = profile.getRestcommHost();
        }
        if (apiHost == null) {
            apiHost = fallbackRestcommBaseUri.getHost();
        }
        // initialize port
        Integer apiPort = null;
        if (workspaceSettings != null && workspaceSettings.getApiServerRestPort() != null) {
            apiPort = workspaceSettings.getApiServerRestPort();
        }
        if (apiPort == null && (profile != null ) && profile.getRestcommPort() != null) {
            apiPort = profile.getRestcommPort();
        }
        if (apiPort == null) {
            apiPort = fallbackRestcommBaseUri.getPort();
        }
        // initialize scheme
        HttpScheme apiScheme = null;
        if (workspaceSettings != null && workspaceSettings.getApiServerScheme() != null )
            apiScheme = workspaceSettings.getApiServerScheme();
        if (apiScheme == null && (profile != null ) && profile.getRestcommScheme() != null )
            apiScheme = profile.getRestcommScheme();
        if (apiScheme == null)
            apiScheme = HttpScheme.valueOf(fallbackRestcommBaseUri.getScheme());
        if (apiScheme == null)
            apiScheme = HttpScheme.http; // default to 'http'

        if (RvdUtils.isEmpty(apiHost) || apiPort == null)
            throw new RestcommClientInitializationException("Could not determine restcomm host/port .");
        // credentials initialization
        String apiUsername = null;
        String apiPassword = null;
        if (profile != null) {
            // initialize username
            apiUsername = profile.getUsername();
            //initialize password
            apiPassword = profile.getToken();
        }
        if (usernameOverride != null) {
            apiUsername = usernameOverride;
            apiPassword = passwordOverride;
        }
        if (RvdUtils.isEmpty(apiUsername))
            throw new RestcommClientInitializationException("Restcomm client could not determine the user for accessing Restcomm");

        this.host = apiHost;
        this.port = apiPort;
        this.protocol = apiScheme.toString();
        this.username = apiUsername;
        this.password = apiPassword;
        apacheClient = CustomHttpClientBuilder.buildHttpClient();
    }

    public RestcommClient (WorkspaceSettings workspaceSettings, UserProfile profile, URI fallbackRestcommBaseUri) throws RestcommClientInitializationException {
        this(workspaceSettings,profile,fallbackRestcommBaseUri,null,null);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Request get(String path) {
        return new Request(this, "GET", path);
    }

    public Request post(String path) {
        return new Request(this, "POST", path);
    }

    public Request delete(String path) {
        return new Request(this, "DELETE", path);
    }

    public void setAuthenticationTokenAsPassword(boolean b) {
        this.authenticationTokenAsPassword = b;
    }



}
