/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.rvd.model.steps.geolocation;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.interpreter.Target;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationStep extends Step {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    String deviceIdentifier;
    String action;
    String method;
    String statusCallback;
    String next;

    public static GeolocationStep createDefault(String name, String phrase) {
        GeolocationStep step = new GeolocationStep();
        step.setName(name);
        step.setLabel("Geolocation");
        step.setKind("Geolocation");
        step.setTitle("Geolocation");

        return step;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatusCallback() {
        return statusCallback;
    }

    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public RcmlGeolocationStep render(Interpreter interpreter) {
        RcmlGeolocationStep rcmlStep = new RcmlGeolocationStep();

        if (!RvdUtils.isEmpty(getNext())) {
            String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".actionhandler";
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", newtarget);
            String action = interpreter.buildAction(pairs);
            rcmlStep.setAction(action);
            rcmlStep.setMethod(getMethod());
        }

        rcmlStep.setDeviceIdentifier(getDeviceIdentifier());
        rcmlStep.setStatusCallback(getStatusCallback());

        return rcmlStep;
    }

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        if (logger.isInfoEnabled()) {
            logger.info("handling geolocation action");
        }
        if (RvdUtils.isEmpty(getNext()))
            throw new InterpreterException("'next' module is not defined for step " + getName());

        String GeolocationSid = interpreter.getRequestParams().getFirst("GeolocationSid");
        String GeolocationStatus = interpreter.getRequestParams().getFirst("GeolocationStatus");

        if (GeolocationSid != null)
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "GeolocationSid", GeolocationSid);
        if (GeolocationStatus != null)
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "GeolocationStatus", GeolocationStatus);

        interpreter.interpret(getNext(), null, null, originTarget);
    }

}
