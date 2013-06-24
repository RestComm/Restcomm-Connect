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

import static com.google.common.base.Preconditions.*;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public class State {
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
  
  @Override public boolean equals(final Object object) {
    if(object == null) {
	  return false;
    } else if(this == object) {
	  return true;
	} else if (getClass() != object.getClass()) {
	  return false;
	}
	final State state = (State)object;
	if(!id.equals(state.getId())) {
	  return false;
	}
	return true;
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
  
  @Override public int hashCode() {
  	final int prime = 5;
  	int result = 1;
  	result = prime * result + id.hashCode();
  	return result;
  }
  
  @Override public String toString() {
    return id;
  }
}
