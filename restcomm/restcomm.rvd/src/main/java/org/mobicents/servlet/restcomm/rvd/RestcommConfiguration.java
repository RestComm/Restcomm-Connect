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

package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.client.utils.URIBuilder;
import org.mobicents.servlet.restcomm.rvd.exceptions.RestcommConfigurationException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.RestcommConfigNotFound;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.RvdErrorParsingRestcommXml;
import org.mobicents.servlet.restcomm.rvd.model.ApiServerConfig;
import org.w3c.dom.Document;

/**
 * @author guilherme.jansen@telestax.com
 */
public class RestcommConfiguration {

    public static ApiServerConfig getApiServerConfig(final String filesystemContextPath) throws RestcommConfigurationException {
        ApiServerConfig config = new ApiServerConfig();

        // Load restcomm configuration. Only the fields we are interested in. See RestcommXml model class
        String restcommConfigPath = filesystemContextPath + "../restcomm.war/WEB-INF/conf/restcomm.xml";
        File file = new File(restcommConfigPath);
        if (!file.exists()) {
            throw new RestcommConfigNotFound("Cannot find restcomm configuration file at: " + restcommConfigPath);
        }
        String recordingsUrl = extractRecordingsUrlFromRestcommConfig(file);

        // Extract the settings we are interested in from the recordings url. We could also any other containing host and port
        // information
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(recordingsUrl);
            config.setHost(uriBuilder.getHost());
            config.setPort(uriBuilder.getPort());
            return config;
        } catch (URISyntaxException e) {
            throw new RvdErrorParsingRestcommXml(
                    "Error extracting host and port information from recordings-uri in restcomm.xml: " + recordingsUrl);
        }
    }

    private static String extractRecordingsUrlFromRestcommConfig(File file) throws RestcommConfigurationException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(file);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/restcomm/runtime-settings/recordings-uri/text()");
            String recordingsUrl = (String) expr.evaluate(doc, XPathConstants.STRING);

            return recordingsUrl.trim();
        } catch (Exception e) {
            throw new RestcommConfigurationException("Error parsing restcomm config file: " + file.getPath(), e);
        }

    }

}
