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
package org.mobicents.servlet.sip.restcomm.fsm;

import java.util.HashSet;
import java.util.Set;

public class State {
  private final String name;
  private final Set<State> transitions;
  
  public State(final String name) {
    super();
    this.name = name;
    this.transitions = new HashSet<State>();
  }
  
  public void addTransition(final State state) {
    transitions.add(state);
  }
  
  @Override public boolean equals(final Object object) {
	if(this == object) {
	  return true;
	}
	if(object == null) {
	  return false;
	}
	if(getClass() != object.getClass()) {
	  return false;
	}
	final State other = (State)object;
	if(name == null) {
	  if(other.getName() != null) {
		return false;
	  }
	} else if(!name.equals(other.getName())) {
		return false;
	}
	return true;
  }
  
  public String getName() {
    return name;
  }
  
  public Set<State> getTransitions() {
    return transitions;
  }
  
  @Override public int hashCode() {
	final int prime = 5;
	int result = 1;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	return result;
  }
}
