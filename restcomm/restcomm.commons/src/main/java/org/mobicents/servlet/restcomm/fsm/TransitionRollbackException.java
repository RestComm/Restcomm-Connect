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
package org.mobicents.servlet.restcomm.fsm;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public final class TransitionRollbackException extends Exception {
    private static final long serialVersionUID = 1290752844732660182L;
    private final Object event;
    private final Transition transition;

    public TransitionRollbackException(final String message, final Object event, final Transition transition) {
        super(message);
        this.event = event;
        this.transition = transition;
    }

    public TransitionRollbackException(final Throwable cause, final Object event, final Transition transition) {
        super(cause);
        this.event = event;
        this.transition = transition;
    }

    public Object getEvent() {
        return event;
    }

    public Transition getTransition() {
        return transition;
    }
}
