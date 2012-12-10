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
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.util.Map;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class TagStrategyFactory {
  private final Map<String, Class<? extends TagStrategy>> strategies;
  
  public TagStrategyFactory(final Map<String, Class<? extends TagStrategy>> strategies) {
    super();
    this.strategies = strategies;
  }
  
  public TagStrategy getTagStrategyInstance(String name) throws TagStrategyInstantiationException {
    if(name == null) {
      throw new NullPointerException("Can not instantiate a strategy for a null tag name.");
	} else if(!strategies.containsKey(name)) {
      throw new TagStrategyInstantiationException("The <" + name + "> tag does not have a suitable strategy.");
	} else {
	  try {
		return strategies.get(name).newInstance();
	  } catch(final InstantiationException exception) {
		throw new TagStrategyInstantiationException(exception);
	  } catch(final IllegalAccessException exception) {
		throw new TagStrategyInstantiationException(exception);
	  }
	}
  }
}
