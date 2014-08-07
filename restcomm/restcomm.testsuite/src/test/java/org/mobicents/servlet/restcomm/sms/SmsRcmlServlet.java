/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a helper class to serve RCML request and also return any custom headers
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class SmsRcmlServlet extends HttpServlet{
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
          BufferedReader reader = req.getReader();
          while ((line = reader.readLine()) != null)
            if (line.contains("SipHeader_X-")){
                String[] parameters = line.split("&");
                for (String parameter: parameters) {
                    if (parameter.startsWith("SipHeader_X-")){
                        String headerName = parameter.split("=")[0].replaceFirst("SipHeader_X-", "X-");
                        String headerValue = URLDecoder.decode(parameter.split("=")[1], "UTF-8");
                        resp.setHeader(headerName, headerValue);
                        
                    }
                }
            }
        } catch (Exception e) { /*report an error*/ }
        
//        while (headers.hasMoreElements()) {
//            String name = headers.nextElement();
//            if (name.startsWith("X-")) {
//                resp.setHeader(name, req.getHeader(name));
//            }
//        }
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<Response>");
        out.println("<Sms to=\"1313\" from=\"+12223334499\">Hello World!</Sms>");
        out.println("</Response>"); 
        out.flush();
    }
    
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException { 
        Enumeration<String> headers = req.getHeaderNames();
        
//        <?xml version="1.0" encoding="UTF-8"?>
//        <Response>
//                    <Sms to="1313" from="+12223334499">Hello World!</Sms>
//        </Response>
        
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        
        while (headers.hasMoreElements()) {
            String name = headers.nextElement();
            if (name.startsWith("X-")) {
                resp.setHeader(name, req.getHeader(name));
            }
        }
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<Response>");
        out.println("<Sms to=\"1313\" from=\"+12223334499\">Hello World!</Sms>");
        out.println("</Response>"); 
        out.flush();
    }
    
}
