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

package org.mobicents.servlet.restcomm.smpp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public final class SmppService extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final ActorSystem system;
    private final Configuration configuration;
    private boolean authenticateUsers = true;
    private final ServletConfig servletConfig;
    private final SipFactory sipFactory;
    private final DaoManager storage;
    private final ServletContext servletContext;
    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;

    public SmppService(final ActorSystem system, final Configuration configuration, final SipFactory factory,
            final DaoManager storage, final ServletContext servletContext) {

        super();
        this.system = system;
        this.configuration = configuration;
        final Configuration runtime = configuration.subset("runtime-settings");
        this.authenticateUsers = runtime.getBoolean("authenticate");
        this.servletConfig = (ServletConfig) configuration.getProperty(ServletConfig.class.getName());
        this.sipFactory = factory;
        this.storage = storage;
        this.servletContext = servletContext;
        
        logger.info("SmppService initalized. Configuration = " + configuration);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        logger.info("onReceive = " + message);
    }
    
    private void initializeSmppConnection(){
        Configuration smsConfiguration = this.configuration.subset("sms-aggregator");
    }

}
