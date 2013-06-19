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
@NotThreadSafe public final class TransitionRollbackException extends Exception {
  private static final long serialVersionUID = 1290752844732660182L;
  private final Object event;
  private final Transition transition;

  public TransitionRollbackException(final String message, final Object event,
      final Transition transition) {
    super(message);
    this.event = event;
    this.transition = transition;
  }

  public TransitionRollbackException(final Throwable cause, final Object event,
      final Transition transition) {
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
