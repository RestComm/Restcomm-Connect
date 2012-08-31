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
package org.mobicents.servlet.sip.restcomm.xml;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class VisitableTag extends AbstractTag {
  private boolean hasBeenVisited;
  
  public VisitableTag() {
    super();
    this.hasBeenVisited = false;
  }
  
  public abstract void accept(TagVisitor visitor) throws VisitorException;
  
  public boolean hasBeenVisited() {
    return hasBeenVisited;
  }
  
  public void setHasBeenVisited(final boolean hasBeenVisited) {
    this.hasBeenVisited = hasBeenVisited;
  }
}
