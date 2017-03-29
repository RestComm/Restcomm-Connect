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
package org.restcomm.connect.extension.multiprovider;

import org.apache.log4j.Logger;

import akka.actor.ActorRef;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.CallRequest;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
//import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionRequest;
import org.restcomm.connect.extension.api.SessionExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.RestcommExtension;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.sms.api.CreateSmsSession;

//import org.restcomm.connect.telephony.api.CreateCall;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletRequest;

//import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;

import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */
@RestcommExtension(author = "abdulazizali", version = "0.0.1.Alpha", type = { ExtensionType.CallManager, ExtensionType.SmsService })
public class MultiProvider implements RestcommExtensionGeneric {

    private static final Logger logger = Logger.getLogger(MultiProvider.class);
    private ServletContext context;
    private ActorRef monitoringService;
    private DaoManager daoManager;
    private String extensionSid;//FIXME

    private HashMap<String, String> outboundSmsTlvMap;
    private HashMap<String, String> outboundProxyMap;
    private HashMap<String, String> serviceProviderMap;
    private JsonParser jsonParser;
    private MultiProviderConfiguration config;
    private String extensionName = "multi_provider";
    private String localConfigPath = "/multi_provider_default_configuration.json";

    public MultiProvider() {
    }

    public MultiProvider(String extensionName, String localConfigPath) {
        // TODO Auto-generated constructor stub
        this.extensionName = extensionName;
        this.localConfigPath = localConfigPath;
    }

    @Override
    public void init(final ServletContext context) {
        try {
            this.context = context;
            daoManager = (DaoManager) this.context.getAttribute(DaoManager.class.getName());
            monitoringService = (ActorRef) context.getAttribute(MonitoringService.class.getName());
            config = new MultiProviderConfiguration();
            config.init(daoManager, extensionName, localConfigPath);
            logger.debug("Sid="+config.getSid());
            extensionSid = config.getSid().toString();
            outboundSmsTlvMap = new HashMap<String,String>();
            jsonParser = new JsonParser();
        } catch (Exception configurationException) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception during multiproviderConfiguration instance");
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return true;// configurationInstance.isEnabled();
    }

    @Override
    public ExtensionResponse preInboundAction(SipServletRequest request) {
        return null;
    }

    @Override
    public ExtensionResponse postInboundAction(SipServletRequest request) {
        return null;
    }

    private Collection loadOutboundSmsMap(final JsonObject json) {
        JsonArray outboundSmsJsonArray = json.getAsJsonArray("outbound-sms");
        Collection nodes = new ArrayList();
        if (outboundSmsJsonArray != null) {
            Iterator<JsonElement> iter = outboundSmsJsonArray.iterator();

            while (iter.hasNext()) {
                JsonElement elem = iter.next();
                String key = elem.getAsJsonObject().entrySet().iterator().next().getKey().toString();
                String val = elem.getAsJsonObject().entrySet().iterator().next().getValue().getAsString();
                logger.debug(key+"="+val);
                ConfigurationNode child = new HierarchicalConfiguration.Node(key);
                child.setValue(val);
                nodes.add(child);
            }
        }
        return nodes;
    }

    @Override
    public ExtensionResponse preOutboundAction(final Object er) {
        ExtensionResponse response = new SessionExtensionResponse();
        boolean allow = true;

        if (!isEnabled()) {
            allow = true;
            response.setAllowed(allow);
        }
        if (er instanceof ExtensionRequest) {
            ExtensionRequest t_er = (ExtensionRequest) er;
            CreateSmsSession createSmsSession = (CreateSmsSession) t_er.getObject();
            String accountSid = createSmsSession.getAccountSid();
            XMLConfiguration cfg = (XMLConfiguration) t_er.getConfiguration();// modify CreateSmsSession
            AccountsDao acd = daoManager.getAccountsDao();
            Account acc =acd.getAccount(new Sid(accountSid));
            logger.debug("account="+acc.toString());

            ExtensionsConfigurationDao ecd = daoManager.getExtensionsConfigurationDao();
            ExtensionConfiguration clob = ecd.getExtensionClob(accountSid, this.extensionSid);
            logger.debug(clob.getConfigurationData());

            String updatedConf = (String) clob.getConfigurationData();

            JsonObject configurationJsonObj = (JsonObject) jsonParser.parse(updatedConf);
            cfg.clearTree("outbound-sms");
            cfg.addNodes("outbound-sms", loadOutboundSmsMap(configurationJsonObj));

            if (logger.isDebugEnabled()) {
                logger.debug("MultiProvider new CreateSmsSession request ");
            }
            response.setObject(cfg);
            response.setAllowed(allow);
        }
        return response;
    }

    @Override
    public ExtensionResponse postOutboundAction(CallRequest callRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtensionResponse preApiAction(ApiRequest apiRequest) {
        ExtensionResponse response = new ExtensionResponse();
        boolean allow = true;

        if (!isEnabled()) {
            allow = true;
            response.setAllowed(allow);
            return response;
        }

        response.setAllowed(allow);
        return response;
    }

    @Override
    public ExtensionResponse postApiAction(ApiRequest apiRequest) {
        ExtensionResponse response = new ExtensionResponse();
        return response;
    }
}
