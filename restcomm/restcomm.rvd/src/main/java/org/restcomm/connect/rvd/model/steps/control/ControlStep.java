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

package org.restcomm.connect.rvd.model.steps.control;

import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ControlStep extends Step {

    public static class Condition {
        String name;
        String operator;
        String operand1;
        String operand2;
    }

    public static class Action {
        String name;
        String kind;
        String param1;
        String param2;
    }

    private List<Condition> conditions;
    private List<Action> actions;
    private String conditionExpression;


    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        return null;
    }

    public String process(Interpreter interpreter, HttpServletRequest httpRequest) throws InterpreterException {

        return null;
    }
}
