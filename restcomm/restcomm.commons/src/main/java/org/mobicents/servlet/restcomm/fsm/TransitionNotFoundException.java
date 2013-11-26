/*
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
package org.mobicents.servlet.restcomm.fsm;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public class TransitionNotFoundException extends Exception {
    private static final long serialVersionUID = -3062590067048473837L;
    private final Object event;
    private final State currrent;
    private final State target;

    public TransitionNotFoundException(final String message, final Object event, final State current, final State target) {
        super(message);
        this.event = event;
        this.currrent = current;
        this.target = target;
    }

    public TransitionNotFoundException(final Throwable cause, final Object event, final State current, final State target) {
        super(cause);
        this.event = event;
        this.currrent = current;
        this.target = target;
    }

    public State getCurrentState() {
        return currrent;
    }

    public Object getEvent() {
        return event;
    }

    public State getTargetState() {
        return target;
    }
}
