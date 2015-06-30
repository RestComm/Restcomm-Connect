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

import static com.google.common.base.Preconditions.*;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public class State {
    private final Action actionOnEnter;
    private final Action actionOnExit;
    private final String id;

    public State(final String id, final Action actionOnEnter, final Action actionOnExit) {
        super();
        checkNotNull(id, "A state can not have a null value for id.");
        this.actionOnEnter = actionOnEnter;
        this.actionOnExit = actionOnExit;
        this.id = id;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null) {
            return false;
        } else if (this == object) {
            return true;
        } else if (getClass() != object.getClass()) {
            return false;
        }
        final State state = (State) object;
        return id.equals(state.getId());
    }

    public Action getActionOnEnter() {
        return actionOnEnter;
    }

    public Action getActionOnExit() {
        return actionOnExit;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 5;
        int result = 1;
        result = prime * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return id;
    }
}
