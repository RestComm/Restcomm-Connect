/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
	
package org.restcomm.connect.testsuite.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import wiremock.org.apache.http.NameValuePair;
import wiremock.org.apache.http.client.ClientProtocolException;
import wiremock.org.apache.http.client.entity.UrlEncodedFormEntity;
import wiremock.org.apache.http.client.methods.CloseableHttpResponse;
import wiremock.org.apache.http.client.methods.HttpDelete;
import wiremock.org.apache.http.client.methods.HttpGet;
import wiremock.org.apache.http.client.methods.HttpPost;
import wiremock.org.apache.http.client.methods.HttpPut;
import wiremock.org.apache.http.impl.client.CloseableHttpClient;
import wiremock.org.apache.http.impl.client.HttpClients;
import wiremock.org.apache.http.message.BasicNameValuePair;

/**
 * @author guilherme.jansen@telestax.com
 */
public class RestcommMultitenancyTool {

    private static RestcommMultitenancyTool instance;

    public static RestcommMultitenancyTool getInstance() {
        if (instance == null) {
            instance = new RestcommMultitenancyTool();
        }
        return instance;
    }

    public int get(String url, String credentialUsername, String credentialPassword) throws ClientProtocolException,
            IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse apiResponse = null;
        HttpGet get = new HttpGet(url);
        get.addHeader("Authorization", "Basic " + getAuthorizationToken(credentialUsername, credentialPassword));
        apiResponse = client.execute(get);
        return apiResponse.getStatusLine().getStatusCode();
    }

    public int post(String url, String credentialUsername, String credentialPassword, HashMap<String, String> params)
            throws ClientProtocolException,
            IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse apiResponse = null;
        HttpPost post = new HttpPost(url);
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        Iterator it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            values.add(new BasicNameValuePair(String.valueOf(pair.getKey()), String.valueOf(pair.getValue())));
        }
        post.setEntity(new UrlEncodedFormEntity(values));
        post.addHeader("Authorization", "Basic " + getAuthorizationToken(credentialUsername, credentialPassword));
        apiResponse = client.execute(post);
        return apiResponse.getStatusLine().getStatusCode();
    }

    public int delete(String url, String credentialUsername, String credentialPassword) throws ClientProtocolException,
            IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse apiResponse = null;
        HttpDelete delete = new HttpDelete(url);
        delete.addHeader("Authorization", "Basic " + getAuthorizationToken(credentialUsername, credentialPassword));
        apiResponse = client.execute(delete);
        return apiResponse.getStatusLine().getStatusCode();
    }

    public int update(String url, String credentialUsername, String credentialPassword, Map<String, String> params) throws ClientProtocolException,
            IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse apiResponse = null;
        HttpPut put = new HttpPut(url);
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        Iterator it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            values.add(new BasicNameValuePair(String.valueOf(pair.getKey()), String.valueOf(pair.getValue())));
        }
        put.setEntity(new UrlEncodedFormEntity(values));
        put.addHeader("Authorization", "Basic " + getAuthorizationToken(credentialUsername, credentialPassword));
        apiResponse = client.execute(put);
        return apiResponse.getStatusLine().getStatusCode();
    }

    private String getAuthorizationToken(String username, String password) {
        byte[] usernamePassBytes = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
        String authenticationToken = Base64.encodeBase64String(usernamePassBytes);
        return authenticationToken;
    }

}
