package org.restcomm.connect.rvd.model.client;

import javax.servlet.http.HttpServletRequest;

import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.interpreter.Target;
import org.restcomm.connect.rvd.interpreter.exceptions.RVDUnsupportedHandlerVerb;
import org.restcomm.connect.rvd.jsonvalidation.ValidationErrorItem;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

import java.util.List;

public abstract class Step {

    private String kind;
    private String label;
    private String title;
    private String name;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract RcmlStep render(Interpreter interpreter) throws InterpreterException;

    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        throw new RVDUnsupportedHandlerVerb();
    }

    /**
     * @returns String - The module name to continue rendering with. null, to continue processing the existing module
     */
    public String process(Interpreter interpreter, HttpServletRequest httpRequest) throws InterpreterException {
        // a placeholder implementation for steps that don't have an actual imlpementation
        return null;
    }

    public List<ValidationErrorItem> validate(String stepPath, Node parentModule) {
        return null; // assume valid unless overriden
    }
}
