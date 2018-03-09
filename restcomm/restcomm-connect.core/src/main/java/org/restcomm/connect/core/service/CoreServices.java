/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
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

package org.restcomm.connect.core.service;

import org.apache.log4j.Logger;
import org.restcomm.connect.core.service.number.NumberSelectorService;
import org.restcomm.connect.dao.DaoManager;

/**
 * @author guilherme.jansen@telestax.com
 */
public class CoreServices {

    private static final Logger logger = Logger.getLogger(CoreServices.class);
    private static CoreServices instance = null;

    // core services
    private NumberSelectorService numberSelector;

    public static CoreServices getInstance() {
        if (instance == null) {
            instance = new CoreServices();
        }
        return instance;
    }

    public void startServices(DaoManager storage) {
        try {
            // core services initialization
            this.numberSelector = new NumberSelectorService(storage.getIncomingPhoneNumbersDao());
        } catch (Exception e) {
            logger.error("Error while initializing core services: ", e);
            throw e;
        }
    }

    public NumberSelectorService getNumberSelector() {
        return numberSelector;
    }

}
