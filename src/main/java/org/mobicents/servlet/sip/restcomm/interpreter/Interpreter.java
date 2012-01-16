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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceDescriptor;
import org.mobicents.servlet.sip.restcomm.http.client.HttpServiceException;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceServer;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagIterator;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;
import org.mobicents.servlet.sip.restcomm.xml.XmlDocument;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RCMLTag;

public final class Interpreter extends FiniteStateMachine implements Runnable, TagVisitor {
  // Logger.
  private static final Logger logger = Logger.getLogger(Interpreter.class);
  // Interpreter states.
  public static final State IDLE = new State("idle");
  public static final State REDIRECTED = new State("redirected");
  public static final State READY = new State("ready");
  public static final State EXECUTING = new State("executing");
  public static final State FINISHED = new State("finished");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(READY);
    READY.addTransition(EXECUTING);
    READY.addTransition(FINISHED);
    EXECUTING.addTransition(READY);
    EXECUTING.addTransition(FINISHED);
    EXECUTING.addTransition(REDIRECTED);
    EXECUTING.addTransition(FAILED);
    REDIRECTED.addTransition(READY);
  }
  
  // Interpreter environment.
  private final InterpreterContext context;
  //Tag strategy factory.
  private final TagStrategyFactory factory;
  // XML Resource.
  private ResourceDescriptor descriptor;
  private XmlDocument resource;
	
  public Interpreter(final InterpreterContext context) {
    super(IDLE);
    addState(IDLE);
    addState(REDIRECTED);
    addState(READY);
    addState(EXECUTING);
    addState(FINISHED);
    addState(FAILED);
    this.context = context;
    this.factory = new TagStrategyFactory();
  }
  
  public void failed() {
    assertState(EXECUTING);
    setState(FAILED);
  }
  
  public void finish() {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(READY);
    possibleStates.add(EXECUTING);
    assertState(possibleStates);
    setState(FINISHED);
  }
  
  public ResourceDescriptor getDescriptor() {
    return descriptor;
  }
  
  public void initialize() throws InterpreterException {
    try {
      final ResourceDescriptor descriptor = context.getEntryPointDescriptor();
      loadResource(descriptor);
      setState(READY);
    } catch(final InterpreterContextException exception) {
      setState(FAILED);
      throw new InterpreterException(exception);
    }
  }
  
  public void loadResource(final ResourceDescriptor descriptor) throws InterpreterException {
	final List<State> possibleStates = new ArrayList<State>();
	possibleStates.add(IDLE);
	possibleStates.add(EXECUTING);
	assertState(possibleStates);
	// Load the XML resource for execution.
	this.descriptor = descriptor;
    final ResourceServer server = context.getResourceServer();
    try {
      resource = server.getXmlResource(descriptor);
  	} catch(final HttpServiceException exception) {
  	  throw new InterpreterException(exception);
  	}
  }
  
  public void redirect() {
    assertState(EXECUTING);
    setState(REDIRECTED);
  }
  
  public void run() {
    while(getState().equals(READY)) {
      TagIterator iterator = resource.iterator();
      while(iterator.hasNext()) {
        final RCMLTag tag = (RCMLTag)iterator.next();
        if(!tag.hasBeenVisited() && tag.isVerb()) {
          // Make sure we're ready to execute the next tag.
          assertState(READY);
          setState(EXECUTING);
          // Try to execute the next tag.
          try {
            tag.accept(this);
          } catch(final VisitorException exception) {
            setState(FAILED);
            logger.error(exception);
          }
          tag.setHasBeenVisited(true);
          // Handle any state changes caused by executing the tag.
          final State state = getState();
          if(state.equals(REDIRECTED)) {
        	iterator = resource.iterator();
        	setState(READY);
          } else if(state.equals(FINISHED) || state.equals(FAILED)) {
            return;
          } else {
            setState(READY);
          }
        }
      }
      setState(FINISHED);
    }
  }

  public void visit(final Tag tag) throws VisitorException {
	try {
	  final TagStrategy strategy = factory.getTagStrategyInstance(tag.getName());
	  strategy.execute(this, context, tag);
	} catch(final TagStrategyInstantiationException exception) {
	  throw new VisitorException(exception);
	} catch(final TagStrategyException exception) {
	  throw new VisitorException(exception);
	}
  }
}
