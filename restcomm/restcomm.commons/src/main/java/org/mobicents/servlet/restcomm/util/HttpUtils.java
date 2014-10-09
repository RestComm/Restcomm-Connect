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
package org.mobicents.servlet.restcomm.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class HttpUtils {
    private HttpUtils() {
        super();
    }

    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static Map<String, String> toMap(final HttpEntity entity) throws IllegalStateException, IOException {

        String contentType = null;
        String charset = null;

        contentType = EntityUtils.getContentMimeType(entity);
        charset = EntityUtils.getContentCharSet(entity);

        List<NameValuePair> parameters = null;
        if (contentType != null && contentType.equalsIgnoreCase(CONTENT_TYPE)) {
            parameters = URLEncodedUtils.parse(entity);
        } else {
            final String content = EntityUtils.toString(entity, HTTP.ASCII);
            if (content != null && content.length() > 0) {
                parameters = new ArrayList<NameValuePair>();
                URLEncodedUtils.parse(parameters, new Scanner(content), charset);
            }
        }

        final Map<String, String> map = new HashMap<String, String>();
        for (final NameValuePair parameter : parameters) {
            map.put(parameter.getName(), parameter.getValue());
        }
        return map;
    }

    public static String toString(final Header[] headers) {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        for (final Header header : headers) {
            parameters.add(new BasicNameValuePair(header.getName(), header.getValue()));
        }
        return URLEncodedUtils.format(parameters, "UTF-8");
    }
}
