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
package org.restcomm.connect.extension.controller;

import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class ExtensionBootstrapper {
    private Logger logger = Logger.getLogger(ExtensionBootstrapper.class);
    private Configuration configuration;
    private ServletContext context;
    List<Object> extensions = new ArrayList<Object>();

    public ExtensionBootstrapper(final ServletContext context, final Configuration configuration) {
        this.configuration = configuration;
        this.context = context;
    }

    public void start() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        List<HierarchicalConfiguration> exts = ((XMLConfiguration)configuration).configurationsAt("extensions.extension");

        for (HierarchicalConfiguration ext: exts) {
            String name = ext.getString("[@name]");
            String className = ext.getString("class");
            boolean enabled = ext.getBoolean("enabled");
            if (enabled) {
                try {
                    Class<RestcommExtensionGeneric> klass = (Class<RestcommExtensionGeneric>) Class.forName(className);
                    RestcommExtensionGeneric extension = klass.newInstance();
                    extension.init(this.context);

                    //Store it in the context using the extension name
                    context.setAttribute(name, extension);
                    ExtensionController.getInstance().registerExtension(extension);
                    if (logger.isInfoEnabled()) {
                        logger.info("Stated Extension: " + name);
                    }
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Exception during initialization of extension \""+name+"\", exception: "+e.getStackTrace());
                    }
                }
            }
        }
    }

}
