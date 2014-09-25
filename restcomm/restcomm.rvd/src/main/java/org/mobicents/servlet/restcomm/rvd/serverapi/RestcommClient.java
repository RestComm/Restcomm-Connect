package org.mobicents.servlet.restcomm.rvd.serverapi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.Gson;

public class RestcommClient {

    private String host;
    private Integer port;
    private String username;
    private String password;
    CloseableHttpClient apacheClient;

    public static class RestcommClientException extends Exception {

        public RestcommClientException(String message, Throwable cause) {
            super(message, cause);
            // TODO Auto-generated constructor stub
        }

        public RestcommClientException(String message) {
            super(message);
            // TODO Auto-generated constructor stub
        }

        public RestcommClientException(Throwable cause) {
            super(cause);
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
            if ( !RvdUtils.isEmpty(name) ) {
                paramNames.add(name);
                paramValues.add(value);
            }
            return this;
        }
        public <T> T done(Gson gson, Class<T> resultClass) throws RestcommClientException {
            // Build the uri for the call made to Restcomm
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setHost(client.host);
            uriBuilder.setPort(client.port);
            uriBuilder.setScheme("http"); // hardcoded
            uriBuilder.setPath(path);

            try {

                CloseableHttpResponse apiResponse;
                if ( "GET".equals(method) ) {
                    for ( int i = 0; i < paramNames.size(); i ++ )
                        uriBuilder.addParameter(paramNames.get(i), paramValues.get(i) );
                    String uri = uriBuilder.build().toString();
                    HttpGet get = new HttpGet( uri );
                    get.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(client.username, client.password));
                    apiResponse = client.apacheClient.execute( get );
                    try {
                        if ( apiResponse.getStatusLine().getStatusCode() != 200 ) {
                            String response_string = IOUtils.toString(apiResponse.getEntity().getContent());
                            throw new RestcommClientException("Error " + apiResponse.getStatusLine().getStatusCode() + " running REST GET request: " + apiResponse.getStatusLine().getReasonPhrase() + " - Response body: " + response_string );
                        }

                        return gson.fromJson( new InputStreamReader(apiResponse.getEntity().getContent()), resultClass );

                    } finally {
                        apiResponse.close();
                    }
                } else
                if ( "POST".equals(method) ) {
                    String uri = uriBuilder.build().toString();
                    HttpPost post = new HttpPost(uri);
                    List <NameValuePair> values = new ArrayList <NameValuePair>();
                    for ( int i = 0; i < paramNames.size(); i ++ ) {
                        values.add( new BasicNameValuePair(paramNames.get(i), paramValues.get(i)) );
                    }
                    post.setEntity(new UrlEncodedFormEntity(values));
                    post.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(client.username, client.password) );
                    apiResponse = client.apacheClient.execute(post);
                    try {
                        if ( apiResponse.getStatusLine().getStatusCode() != 200 ) {
                            String response_string = IOUtils.toString(apiResponse.getEntity().getContent());
                            throw new RestcommClientException("ApiServer could not create the call: " + apiResponse.getStatusLine().getReasonPhrase() + " - Response body: " +  response_string);
                        }
                        String content = IOUtils.toString(apiResponse.getEntity().getContent());

                        //return gson.fromJson( new InputStreamReader(apiResponse.getEntity().getContent()), resultClass );
                        return gson.fromJson( content, resultClass );

                    } finally {
                        apiResponse.close();
                    }
                } else
                    throw new UnsupportedOperationException("Only GET and POST methods are supported");

            } catch (IOException e) {
                throw new RestcommClientException(e);
            } catch (URISyntaxException e) {
                throw new RestcommClientException(e);
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


}
