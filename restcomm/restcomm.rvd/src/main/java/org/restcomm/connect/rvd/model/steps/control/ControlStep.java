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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ControlStep extends Step {

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
                    if ("AND".equals(previousOperation))
                        result = result && evaluateCondition(getConditionByName(part), interpreter);
                    else
                    if ("OR".equals(previousOperation))
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
                    String next = executeAction(action, interpreter);
                    // redirect to next module if a non-null value is returned
                    if (next != null) {
                        return next;
                    }
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

    boolean evaluateCondition(Condition condition, Interpreter interpreter) throws InterpreterException {
        if (condition.operator.equals("matches")) {
            String textExpanded = interpreter.populateVariables(condition.matcher.text);
            // regex expressions don't support RVD variables
            Pattern pattern = Pattern.compile(condition.matcher.regex);
            return pattern.matcher(textExpanded).matches();
        } else {
            String operand1Expanded = interpreter.populateVariables(condition.comparison.operand1);
            String operand2Expanded = interpreter.populateVariables(condition.comparison.operand2);
            if (condition.comparison.type == Condition.Comparison.ComparisonType.text || condition.comparison.type == null) { // if no type is defined compare as strings
                switch (condition.operator) {
                    case "equals":
                        return operand1Expanded.equals(operand2Expanded);
                    case "notequal":
                        return !operand1Expanded.equals(operand2Expanded);
                    case "greater":
                        return (operand1Expanded.compareTo(operand2Expanded) > 0);
                    case "greaterEqual":
                        return (operand1Expanded.compareTo(operand2Expanded) >= 0);
                    case "less":
                        return (operand1Expanded.compareTo(operand2Expanded) < 0);
                    case "lessEqual":
                        return (operand1Expanded.compareTo(operand2Expanded) >= 0);
                }
            } else
            if (condition.comparison.type == Condition.Comparison.ComparisonType.numeric) {
                try {
                    Float operand1Float = Float.parseFloat(operand1Expanded);
                    Float operand2Float = Float.parseFloat(operand2Expanded);
                    switch (condition.operator) {
                        case "equals":
                            return operand1Float.equals(operand2Float);
                        case "notequal":
                            return !operand1Float.equals(operand2Float);
                        case "greater":
                            return operand1Float > operand2Float;
                        case "greaterEqual":
                            return operand1Float >= operand2Float;
                        case "less":
                            return operand1Float < operand2Float;
                        case "lessEqual":
                            return operand1Float <= operand2Float;
                    }
                } catch (NumberFormatException e) {
                    throw new InterpreterException("Cannot parse numeric comparison operands. Operand1: " + operand1Expanded + " . Operand2: " + operand2Expanded);
                }
            }
        }
        throw new NotImplementedException();
    }

    // returns the module to redirect to if applicable
    String executeAction(Action action, Interpreter interpreter ) {
        if (action.continueTo != null) {
            return action.continueTo.target;
        } else
        if (action.assign != null) {
            String expandedSource = interpreter.populateVariables(action.assign.expression);
            // TODO think over the module/application variable scope
            if (action.assign.varScope == null || action.assign.varScope.equals(VariableScopes.mod))
                interpreter.putModuleVariable(action.assign.varName, expandedSource);
            else
                interpreter.putStickyVariable(action.assign.varName, expandedSource);
        } else
        if (action.capture != null) {
            String expandedData = interpreter.populateVariables(action.capture.data);
            executeCaptureAction(expandedData, action.capture.regex, action.capture.varName, action.capture.varScope, interpreter);
        }
        // TODO handle other cases with not-implemented exception
        return null;
    }

    private void executeCaptureAction(String data, String regex, String variable, VariableScopes variableScope, Interpreter interpreter) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        if (matcher.matches()) {
            String captured = matcher.group(1); // by convention get group 1 i.e. first pair of parenthesis
            if (variableScope == null || variableScope.equals(VariableScopes.mod))
                interpreter.putModuleVariable(variable, captured);
            else
                interpreter.putStickyVariable(variable, captured);
        }
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
                // either comparison/matcher should be defined
                if ( condition.comparison == null && condition.matcher == null) {
                    errorItems.add(new ValidationErrorItem("error","Condition incomplete",stepPath));
                }
            }
        }
        if (actions != null && actions.size() > 0) {
            for (Action action: actions) {
                if (action.continueTo != null) {
                    if (RvdUtils.isEmpty(action.continueTo.target)) {
                        errorItems.add(new ValidationErrorItem("error","No target module specified",stepPath));
                    } else
                    if (action.continueTo.target.equals(module.getName()))
                        errorItems.add(new ValidationErrorItem("error","Cyclic module execution detected",stepPath));
                } else
                if (action.assign != null) {
                    if (RvdUtils.isEmpty(action.assign.varName)) {
                        errorItems.add(new ValidationErrorItem("error","Assignment misses destination",stepPath));
                    }
                } else
                if (action.capture != null) {
                    if (RvdUtils.isEmpty(action.capture.varName))
                        errorItems.add(new ValidationErrorItem("error","Missing capture action variable",stepPath));
                }
            }
        }
        return errorItems;
    }

    public static class Condition {

        String name;
        String operator;
        Comparison comparison; // one of comparison/matcher is enabled at a time
        Matcher matcher;

        public enum Operators {
            equals,
            greater,
            greaterEqual,
            less,
            lessEqual,
            matches

        }
        public static class Comparison {
            String operand1;
            String operand2;
            ComparisonType type;

            public enum ComparisonType {
                text, numeric
            }

        }
        public static class Matcher {
            String text;
            String regex;

        }
    }

    public static class Action {
        String name;
        AssignParams assign;
        ContinueToParams continueTo;
        CaptureParams capture;
    }

    public enum VariableScopes {
        mod,
        app
    }

    public static class AssignParams {
        String expression;
        String varName;
        VariableScopes varScope;
    }

    public static class ContinueToParams {
        String target;
    }

    public static class CaptureParams {
        String regex;
        String data;
        String varName;
        VariableScopes varScope;
    }
}
