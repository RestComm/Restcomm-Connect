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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;
import org.mobicents.servlet.sip.restcomm.util.HttpUtils;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagIterator;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocument;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocumentBuilder;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTagFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RcmlInterpreter extends FiniteStateMachine implements Runnable, TagVisitor {
  // Logger.
  private static final Logger logger = Logger.getLogger(RcmlInterpreter.class);
  // RcmlInterpreter states.
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
  private static final List<NameValuePair> EMPTY_NAME_VALUE_PAIRS = new ArrayList<NameValuePair>(0);
  
  // RcmlInterpreter environment.
  private final RcmlInterpreterContext context;
  //Tag strategy strategies.
  private final TagStrategyFactory strategies;
  // XML Resource Builder.
  private final RcmlDocumentBuilder resourceBuilder;
  // XML Resource.
  private RcmlDocument resource;
  // XML Resource request attributes.
  private String resourceRequestVariables;
  // XML Resource request resourceRequestMethod.
  private String resourceRequestMethod;
  // XML Resource response headers.
  private String resourceResponseHeaders;
  //XML Resource URI.
  private URI resourceUri;
	
  public RcmlInterpreter(final RcmlInterpreterContext context) {
    super(IDLE);
    addState(IDLE);
    addState(REDIRECTED);
    addState(READY);
    addState(EXECUTING);
    addState(FINISHED);
    addState(FAILED);
    this.context = context;
    this.strategies = new TagStrategyFactory();
    this.resourceBuilder = new RcmlDocumentBuilder(new RcmlTagFactory());
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
  
  public String getCurrentResource() {
    return resource.toString();
  }
  
  public String getCurrentResourceRequestMethod() {
    return resourceRequestMethod;
  }
  
  public String getCurrentResourceRequestVariables() {
    return resourceRequestVariables;
  }
  
  public String getCurrentResourceResponseHeaders() {
    return resourceResponseHeaders;
  }
  
  public URI getCurrentResourceUri() {
    return resourceUri;
  }
  
  public void initialize() throws InterpreterException {
	try {
	  loadResource(context.getVoiceUrl(), context.getVoiceMethod());
	} catch(final InterpreterException exception) {
	  logger.error("Using fallback url the default voice url failed.", exception);
	  loadResource(context.getVoiceFallbackUrl(), context.getVoiceFallbackMethod());
	}
    setState(READY);
  }
  
  public void loadResource(final URI uri, final String fetchMethod) throws InterpreterException {
    loadResource(uri, fetchMethod, EMPTY_NAME_VALUE_PAIRS);
  }
  
  public void loadResource(final URI uri, final String fetchMethod, List<NameValuePair> additionalParameters) throws InterpreterException {
	final List<State> possibleStates = new ArrayList<State>();
	possibleStates.add(IDLE);
	possibleStates.add(EXECUTING);
	assertState(possibleStates);
	// Load the XML resource for execution.
	final List<NameValuePair> parameters = context.getRcmlRequestParameters();
	parameters.addAll(additionalParameters);
	resourceRequestVariables = URLEncodedUtils.format(parameters, "UTF-8");
	try {
	  HttpUriRequest request = null;
	  if(RequestMethod.GET.equals(fetchMethod)) {
	    request = new HttpGet(URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(),
	        resourceRequestVariables, null));
	  } else if(RequestMethod.POST.equals(fetchMethod)) {
	    request = new HttpPost(uri);
	    ((HttpPost)request).setEntity(new UrlEncodedFormEntity(parameters));
	  }
	  final HttpClient client = new DefaultHttpClient();
	  final HttpResponse response = client.execute(request);
	  final int status = response.getStatusLine().getStatusCode();
	  if(status == HttpStatus.SC_OK) {
		this.resourceResponseHeaders = HttpUtils.toString(response.getAllHeaders());
	    this.resource = resourceBuilder.build(response.getEntity().getContent());
	    this.resourceRequestMethod = fetchMethod;
	    this.resourceUri = uri;
	    if(logger.isInfoEnabled()) {
	      final StringBuilder buffer = new StringBuilder();
	      buffer.append("Successfully Loaded Resource\n");
	      buffer.append("URI: ").append(resourceUri.toString()).append("\n");
	      buffer.append("Fetch Method: ").append(resourceRequestMethod).append("\n");
	      buffer.append(resource.toString());
	      logger.info(buffer.toString());
	    }
	  } else {
		final String reason = response.getStatusLine().getReasonPhrase();
		final StringBuilder buffer = new StringBuilder();
		buffer.append(status).append(" ").append(reason);
	    throw new InterpreterException(buffer.toString());
	  }
	} catch(final Exception exception) {
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
        final RcmlTag tag = (RcmlTag)iterator.next();
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
    if(tag instanceof RcmlTag) {
      final RcmlTag rcmlTag = (RcmlTag)tag;
	  try {
	    final TagStrategy strategy = strategies.getTagStrategyInstance(tag.getName());
	    strategy.initialize(this, context, rcmlTag);
	    strategy.execute(this, context, rcmlTag);
	  } catch(final Exception exception) {
	    logger.error(exception);
	    throw new VisitorException(exception);
	  }
    }
  }
}
