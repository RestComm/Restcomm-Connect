package org.mobicents.servlet.restcomm.rvd.model.client;

import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.Target;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RVDUnsupportedHandlerVerb;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

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

    // a placeholder function for steps that don't have an actual imlpementation
    public String process(Interpreter interpreter, HttpServletRequest httpRequest ) throws InterpreterException { return null; }
}
