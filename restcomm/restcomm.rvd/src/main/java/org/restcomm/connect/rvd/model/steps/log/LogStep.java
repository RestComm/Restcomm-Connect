/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.rvd.model.steps.log;

import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;

import javax.servlet.http.HttpServletRequest;

public class LogStep extends Step {

    private String message;

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String process(Interpreter interpreter, HttpServletRequest httpRequest ) throws InterpreterException {
        if ( interpreter.getRvdContext().getProjectSettings().getLogging() ) {
            String expandedMessage = interpreter.populateVariables(message);
            interpreter.getProjectLogger().log(expandedMessage).tag("app",interpreter.getAppName()).tag("ES").tag("LOG").done();
        }
        return null;
    }

}
