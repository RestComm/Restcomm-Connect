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
import org.restcomm.connect.rvd.jsonvalidation.ValidationErrorItem;
import org.restcomm.connect.rvd.model.client.Node;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;
import org.restcomm.connect.rvd.utils.RvdUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.restcomm.connect.rvd.model.steps.control.ControlStep.Condition.variableScopes;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ControlStep extends Step {

    public static class Condition {
        public static List<String> unaryOperator = Arrays.asList("isTrue","isFalse");
        public static List<String> binaryOperator = Arrays.asList("equals","greater","less");
        public static List<String> variableScopes = Arrays.asList("mod","app");

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
        String param3; // this holds the variable-scope property
    }

    private List<Condition> conditions;
    private List<Action> actions;
    private String conditionExpression;


    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        return null;
    }

    public String process(Interpreter interpreter, HttpServletRequest httpRequest) throws InterpreterException {
        // evaluate conditions
        Boolean result = true; // default to true in case no conditions have been specified
        if (conditionExpression != null) {
            String[] parts = conditionExpression.split(" ");
            String part;
            int i = 0;
            String previousOperation = null;
            while (i < parts.length) {
                part = parts[i];
                if (part.startsWith("C")) { // is this a condition
                    if (previousOperation == null)
                        result = evaluateCondition(getConditionByName(part), interpreter);
                    else
                    if (previousOperation == "AND")
                        result = result && evaluateCondition(getConditionByName(part), interpreter);
                    else
                    if (previousOperation == "OR")
                        result = result || evaluateCondition(getConditionByName(part), interpreter);
                    else
                        // TODO return a proper error here
                        throw new RuntimeException("Invalid conditionExpression: " + conditionExpression);
                } else
                if (part.equals("AND") || part.equals("OR")) {
                    previousOperation = part;
                }
                i++;
            }
        }
        // execute actions
        if (result) {
            if (actions != null && actions.size() > 0) {
                for (Action action : actions) {
                    executeAction(action, interpreter);
                }
            }
        }



        return null;
    }

    Condition getConditionByName(String name) {
        if (conditions != null) {
            for (Condition condition: conditions) {
                if (condition.name.equals(name))
                    return condition;
            }
        }
        return null;
    }

    boolean evaluateCondition(Condition condition, Interpreter interpreter) {
        String operand1Expanded = interpreter.populateVariables(condition.operand1);
        if (Condition.binaryOperator.contains(condition.operator)) {
            String operand2Expanded = interpreter.populateVariables(condition.operand2);
            switch (condition.operator) {
                case "equals":
                    return operand1Expanded.equals(operand2Expanded);
                case "greater":
                    return Float.parseFloat(operand1Expanded) > Float.parseFloat(operand2Expanded);
                case "less":
                    return Float.parseFloat(operand1Expanded) < Float.parseFloat(operand2Expanded);
            }
        }
        throw new NotImplementedException();
    }

    // returns the module to redirect to if applicable
    String executeAction(Action action, Interpreter interpreter ) {
        switch (action.kind) {
            case "continueTo":
                return action.param1;
            case "assign":
                String expandedSource = interpreter.populateVariables(action.param1);
                // TODO think over the module/application variable scope
                if (RvdUtils.isEmpty(action.param3) || "mod".equals(action.param3))
                    interpreter.putModuleVariable(action.param2, expandedSource);
                else
                    interpreter.putStickyVariable(action.param2, expandedSource);

                break;
            // TODO handle other cases with not-implemented exception
        }
        return null;
    }

    /**
     * Checks for semantic validation error in the state object and returns them as ErrorItems. If no error
     * is detected an empty list is returned
     *
     * @return a list of ValidationErrorItem objects or an empty list
     * @param stepPath
     * @param module
     */
    @Override
    public List<ValidationErrorItem> validate(String stepPath, Node module) {
        List<ValidationErrorItem> errorItems = new ArrayList<ValidationErrorItem>();
        if (conditions != null && conditions.size() > 0) {
            for (Condition condition: conditions) {
                // if the operator is binary, both operands should exist
                if (Condition.binaryOperator.contains(condition.operator)) {
                    if ( RvdUtils.isEmpty(condition.operand1) )
                        errorItems.add(new ValidationErrorItem("error","operand1 is not specified",stepPath));
                    if (RvdUtils.isEmpty(condition.operand2 ))
                        errorItems.add(new ValidationErrorItem("error","operand2 is not specified",stepPath));
                } else
                if (Condition.unaryOperator.contains(condition.operator)) {
                    if ( RvdUtils.isEmpty(condition.operand1 )) {
                        errorItems.add(new ValidationErrorItem("error","operand1 is not specified",stepPath));
                    }
                }
            }
        }
        if (actions != null && actions.size() > 0) {
            for (Action action: actions) {
                if (action.kind.equals("continueTo")) {
                    if (RvdUtils.isEmpty(action.param1)) {
                        errorItems.add(new ValidationErrorItem("error","No target module specified",stepPath));
                    } else
                    if (action.param1.equals(module.getName()))
                        errorItems.add(new ValidationErrorItem("error","Cyclic module execution detected",stepPath));
                } else
                if (action.kind.equals("assign")) {
                    if (RvdUtils.isEmpty(action.param1)) {
                        errorItems.add(new ValidationErrorItem("error","Assignment misses source",stepPath));
                    }
                    if (RvdUtils.isEmpty(action.param2)) {
                        errorItems.add(new ValidationErrorItem("error","Assignment misses destination",stepPath));
                    }
                    if (!RvdUtils.isEmpty(action.param3)) {
                        if (!variableScopes.contains(action.param3))
                            errorItems.add(new ValidationErrorItem("error","Invalid variable scope: " + action.param3,stepPath));
                    }
                }
            }
        }
        return errorItems;
    }
}
