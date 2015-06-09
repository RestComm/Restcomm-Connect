/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.http;

/**
 * @author liblefty@gmail.com
 * */

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//Http request
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.http.client.HttpRequestDescriptor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Path("/ProductRegister")
public class ProductRegisterEndPoint extends AbstractEndpoint {
    private static Logger logger = Logger.getLogger(AnnouncementsEndpoint.class);
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response doGet(@Context HttpServletRequest request) {
    try {
        String sUrl = "https://script.google.com/macros/s/AKfycbylNnJkYf3hTI_w0TwlVZu4I0FHJBOkcaGB83J6PD_9CODLMciY/exec";
        String reportsStr = "?";

        List<NameValuePair> Params;
        List<NameValuePair> TmpParams = new ArrayList<>();


        final URI uri = URI.create(sUrl);
        final String method = "GET";
        Enumeration ParEnum = request.getParameterNames();

       while(ParEnum.hasMoreElements()) {
            Object key = ParEnum.nextElement();
           TmpParams.add(new BasicNameValuePair(key.toString(), request.getParameter(key.toString())));
            logger.info("Key: " + key);
            logger.info("Val: " + request.getParameter(key.toString()));
        }

        final HttpRequestDescriptor req = new HttpRequestDescriptor(uri, method,TmpParams);
        Params = req.getParameters();

        for(int i = 0; i < Params.size(); i++) {
            reportsStr += Params.get(i);
            if(i != Params.size() - 1) {
                reportsStr += "&";
            }
        }

        HttpClient client = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(req.getUri()+reportsStr);
        HttpResponse response = client.execute(httpget);
        String json = EntityUtils.toString(response.getEntity());
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }catch(Exception e){
        String json_response = "{\"success\":" + false + "}";
        return Response.ok(json_response, MediaType.APPLICATION_JSON).build();
    }

    }
}


