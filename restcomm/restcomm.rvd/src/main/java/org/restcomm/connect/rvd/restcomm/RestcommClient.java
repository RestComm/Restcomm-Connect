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

package org.restcomm.connect.rvd.restcomm;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
import org.restcomm.connect.rvd.exceptions.AccessApiException;
import org.restcomm.connect.rvd.commons.http.CustomHttpClientBuilder;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.utils.RvdUtils;

import com.google.gson.Gson;

/**
 * @author Orestis Tsakiridis
 */
public class RestcommClient {

    private final URI restcommBaseUrl;
    private final String authHeader;
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
            URIBuilder uriBuilder = new URIBuilder(client.getRestcommBaseUrl());
            uriBuilder.setPath(path);

            try {

                CloseableHttpResponse apiResponse;
                if ("GET".equals(method)) {
                    for (int i = 0; i < paramNames.size(); i++)
                        uriBuilder.addParameter(paramNames.get(i), paramValues.get(i));
                    String uri = uriBuilder.build().toString();
                    HttpGet get = new HttpGet(uri);
                    get.addHeader("Authorization", client.authHeader);
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
                    post.addHeader("Authorization", client.authHeader);
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

                        // handle WebTrigger Create Call responses in a special way.
                        if ( resultClass.equals(RestcommCallArray.class) ) {
                            // we need to take care of two different types of responses i.e. object and array
                            RestcommCallArray calls = new RestcommCallArray();
                            JsonParser parser = new JsonParser();
                            JsonElement element = parser.parse(content);
                            if (element.isJsonObject()) {
                                RestcommCall call = gson.fromJson(content,RestcommCall.class);
                                calls.add(call);
                            } else
                            if (element.isJsonArray()) {
                                JsonArray array = element.getAsJsonArray();
                                for (int i = 0; i < array.size(); i++) {
                                    calls.add(gson.fromJson(array.get(i), RestcommCall.class));
                                }
                            } else {
                                throw new RuntimeException("Invalid response format returned from Restcomm");
                            }
                            return (T) calls;
                        } else
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
                    delete.addHeader("Authorization", client.authHeader);
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
    }

    /**
     * @param restcommBaseUri
     * @throws RestcommClientInitializationException
     */
    public RestcommClient (URI restcommBaseUri, String authHeader, CustomHttpClientBuilder httpClientbuilder) throws RestcommClientInitializationException {
        if (RvdUtils.isEmpty(authHeader))
            throw new RestcommClientInitializationException("Restcomm client could not determine the user for accessing Restcomm");
        this.authHeader = authHeader;
        this.restcommBaseUrl = restcommBaseUri;
        apacheClient = httpClientbuilder.buildHttpClient();
    }

    public URI getRestcommBaseUrl() {
        return restcommBaseUrl;
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
}
