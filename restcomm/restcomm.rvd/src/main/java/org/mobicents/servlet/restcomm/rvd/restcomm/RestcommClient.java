package org.mobicents.servlet.restcomm.rvd.restcomm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.rvd.exceptions.AccessApiException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.Gson;

public class RestcommClient {

    private String host;
    private Integer port;
    private String username;
    private String password;
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
            uriBuilder.setScheme("http"); // hardcoded
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
                        String content = IOUtils.toString(apiResponse.getEntity().getContent());
                        return gson.fromJson(content, resultClass);
                    } finally {
                        apiResponse.close();
                    }
                } else
                    throw new UnsupportedOperationException("Only GET, POST and DELETE methods are supported");

            } catch (IOException e) {
                throw new RestcommClientException("Error building URL from this path: " + path, e);
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

    public RestcommClient(String host, int port, String username, String password) {
        // TODO Auto-generated constructor stub
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        apacheClient = HttpClients.createDefault();
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
