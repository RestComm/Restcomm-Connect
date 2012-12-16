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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.http.HttpRequestDescriptor;
import org.mobicents.servlet.sip.restcomm.interpreter.http.HttpRequestExecutor;
import org.mobicents.servlet.sip.restcomm.interpreter.http.HttpResponseDescriptor;
import org.mobicents.servlet.sip.restcomm.util.HttpUtils;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocument;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocumentBuilder;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocumentBuilderException;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagIterator;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTagFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class RcmlInterpreter extends FiniteStateMachine implements Runnable, TagVisitor {
	private static final Logger logger = Logger.getLogger(RcmlInterpreter.class);
	// RcmlInterpreter states.
	protected static final State IDLE = new State("idle");
	protected static final State REDIRECTED = new State("redirected");
	protected static final State READY = new State("ready");
	protected static final State EXECUTING = new State("executing");
	protected static final State FINISHED = new State("finished");
	protected static final State FAILED = new State("failed");
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

	// Configuration
	private final Configuration configuration;
	// RcmlInterpreter environment.
	private final RcmlInterpreterContext context;
	//Data Access Object Manager.
	private final DaoManager daos;
	//Tag strategy strategies.
	private final TagStrategyFactory strategies;
	// XML Resource Builder.
	private final RcmlDocumentBuilder resourceBuilder;
	// XML Resource.
	private RcmlDocument resource;
	// XML Resource URI.
	private URI resourceUri;
	// XML Resource request attributes.
	private String requestParameters;
	// XML Resource request requestMethod.
	private String requestMethod;
	// XML Resource response body.
	private String responseBody;
	// XML Resource response headers.
	private String responseHeaders;
	// The thread executing this interpreter.
	private Thread thread;

	public RcmlInterpreter(final RcmlInterpreterContext context,
	    final TagStrategyFactory strategies) {
	  super(IDLE);
	  addState(IDLE);
	  addState(REDIRECTED);
	  addState(READY);
	  addState(EXECUTING);
	  addState(FINISHED);
	  addState(FAILED);
	  
	  this.context = context;
	  this.strategies = strategies;
	  this.resourceBuilder = new RcmlDocumentBuilder(new RcmlTagFactory());
	  final ServiceLocator services = ServiceLocator.getInstance();
	  configuration = services.get(Configuration.class);
	  daos = services.get(DaoManager.class);
	}

	protected abstract void cleanup();

	public void failed() {
		assertState(EXECUTING);
		setState(FAILED);
	}

	public void finish() {
		final State state = getState();
		if(!FINISHED.equals(state) && !FAILED.equals(state)) {
		  setState(FINISHED);
		}
	}
	
	public void finishAndInterrupt() {
	  finish();
	  interruptThread();
	}

	public URI getCurrentResourceUri() {
		return resourceUri;
	}

	private URI getMoreInfo(final int errorCode) {
		String errorDictionary = configuration.getString("error-dictionary-uri");
		errorDictionary = StringUtils.addSuffixIfNotPresent(errorDictionary, "/");
		final StringBuilder buffer = new StringBuilder();
		buffer.append(errorDictionary).append(errorCode).append(".html");
		return URI.create(buffer.toString());
	}
	
	private void interruptThread() {
	  if(thread != null) {
	    final Thread.State state = thread.getState();
	    if(Thread.State.BLOCKED == state || Thread.State.TIMED_WAITING == state ||
            Thread.State.WAITING == state) {
	      thread.interrupt();
	    }
	  }
	}

	protected abstract void initialize();
	
	public boolean isRunning() {
      final State state = getState();
      return READY.equals(state) || EXECUTING.equals(state) || REDIRECTED.equals(state);
	}
	
	public synchronized void join() throws InterruptedException {
	  if(isRunning()) { wait(); }
	}

	public void load(final URI uri, final String method, List<NameValuePair> parameters)
			throws InterpreterException {
		final List<State> possibleStates = new ArrayList<State>();
		possibleStates.add(IDLE);
		possibleStates.add(EXECUTING);
		assertState(possibleStates);
		// Load the XML resource for execution.
		HttpRequestDescriptor request = null;
		try { request = new HttpRequestDescriptor(uri, method, parameters); }
		catch(final UnsupportedEncodingException ignored) { }
		catch(final URISyntaxException exception) {
		  save(notify(context, Notification.ERROR, 11100));
		  throw new InterpreterException(exception);
		}
		resourceUri = request.getUri();
		requestMethod = request.getMethod();
		requestParameters = URLEncodedUtils.format(request.getParameters(), "UTF-8");
		try {
		  final HttpRequestExecutor executor = new HttpRequestExecutor();
		  final HttpResponseDescriptor response = executor.execute(request);
		  responseHeaders = HttpUtils.toString(response.getHeaders());
		  if(HttpStatus.SC_OK == response.getStatusCode() &&
		      (response.getContentLength() > 0 || response.isChunked())) {
		    final String contentType = response.getContentType();
		    if(contentType.contains("text/xml") || contentType.contains("application/xml") ||
		        contentType.contains("text/html")) {
		      final String data = StringUtils.toString(response.getContent());
			  responseBody = data;
		      resource = resourceBuilder.build(data);
		    } else if(contentType.contains("audio/wav") || contentType.contains("audio/wave") ||
		        contentType.contains("audio/x-wav")) {
		      resource = resourceBuilder.build(loadWav(response.getContent()));
		    } else if(contentType.contains("text/plain")) { 
		      final String data = StringUtils.toString(response.getContent());
			  responseBody = data;
			  resource = resourceBuilder.build(loadPlainText(data));
		    } else {
		      save(notify(context, Notification.ERROR, 12300));
		      throw new InterpreterException("Invalid content type " + contentType);
		    }
			if(!"Response".equals(resource.getName())) {
			  save(notify(context, Notification.ERROR, 12102));
			  throw new InterpreterException("Invalid document root for document located @ " +
			      request.getUri().toString());
			}
		  } else {
			save(notify(context, Notification.ERROR, 11200));
			throw new InterpreterException("Received an unsucessful response when requesting a document @ " +
			    request.getUri().toString());
		  }
		} catch(final IOException exception) {
		  save(notify(context, Notification.ERROR, 11200));
		  throw new InterpreterException(exception);
		} catch(final RcmlDocumentBuilderException exception) {
		  save(notify(context, Notification.ERROR, 12100));
		  throw new InterpreterException(exception);
		} catch(final URISyntaxException exception) {
		  save(notify(context, Notification.ERROR, 11100));
		  throw new InterpreterException(exception);
		}
	}
	
	public String loadPlainText(final String text) {
	  return new StringBuilder().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
		  .append("<Response><Say>").append(text).append("</Say></Response>").toString();
	}
	
	public String loadWav(final InputStream input) {
	  return new StringBuilder().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
	      .append("<Response><Play>").append("</Play></Response>").toString();
	}

	public Notification notify(final RcmlInterpreterContext context, final int log, final int errorCode) {
	    return notify(context, log, errorCode, resourceUri, requestMethod, requestParameters, responseBody,
		    responseHeaders);
	}

	public Notification notify(final RcmlInterpreterContext context, final int log, final int errorCode,
			final URI resourceUri, final String requestMethod, final String requestVariables,
			final String responseBody, final String responseHeaders) {
		final Notification.Builder builder = Notification.builder();
		final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
		builder.setSid(sid);
		builder.setAccountSid(context.getAccountSid());
		builder.setApiVersion(context.getApiVersion());
		builder.setLog(log);
		builder.setErrorCode(errorCode);
		builder.setMoreInfo(getMoreInfo(errorCode));
		// Fix Me: This is going to have to be resolved in Beta 2 release. Create proper message text.
		// This has been documented in Issue# 55 @ http://code.google.com/p/restcomm/issues/detail?id=55
		builder.setMessageText(new String());
		builder.setMessageDate(DateTime.now());
		builder.setRequestUrl(resourceUri);
		builder.setRequestMethod(requestMethod);
		builder.setRequestVariables(requestVariables);
		builder.setResponseBody(responseBody);
		builder.setResponseHeaders(responseHeaders);
		String rootUri = configuration.getString("root-uri");
		rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
		final StringBuilder buffer = new StringBuilder();
		buffer.append(rootUri).append(context.getApiVersion()).append("/Accounts/");
		buffer.append(context.getAccountSid().toString()).append("/Notifications/");
		buffer.append(sid.toString());
		final URI uri = URI.create(buffer.toString());
		builder.setUri(uri);
		return  builder.build();
	}

	public void redirect() {
		assertState(EXECUTING);
		setState(REDIRECTED);
	}
	
	public void redirectAndInterrupt() {
      redirect();
      interruptThread();
	}

	public void run() {
	  // Make sure we keep a reference to the thread executing
	  // this interpreter so we can interrupt it if necessary.
	  thread = Thread.currentThread();
	  // Initialize the interpreter.
	  initialize();
	  while(getState().equals(READY)) {
	    // Start executing the document.
	    TagIterator iterator = resource.iterator();
	    while(iterator.hasNext()) {
		  final RcmlTag tag = (RcmlTag)iterator.next();
		  if(!tag.hasBeenVisited() && tag.isVerb()) {
		    // Make sure we're ready to execute the next tag.
			assertState(READY);
			setState(EXECUTING);
			// Try to execute the next tag.
			try { tag.accept(this); }
			catch(final VisitorException exception) { logger.warn(exception); }
			tag.setHasBeenVisited(true);
			// Handle any state changes caused by executing the tag.
			final State state = getState();
			if(state.equals(REDIRECTED)) {
			  iterator = resource.iterator();
			  setState(READY);
			} else if(state.equals(FINISHED) || state.equals(FAILED)) {
			  break;
			} else {
			  setState(READY);
			}
		  }
		}
	    finish();
	  }
	  cleanup();
	  notifyAll();
	  thread = null;
	}
	
	public void save(final Notification notification) {
      final NotificationsDao dao = daos.getNotificationsDao();
      dao.addNotification(notification);
	}
	
	public void sendStatusCallback() {
	  final URI uri = context.getStatusCallback();
	  final String method = context.getStatusCallbackMethod();
	  final List<NameValuePair> parameters = context.getRcmlRequestParameters();
	  if(uri != null) {
		try {
		  final HttpRequestExecutor executor = new HttpRequestExecutor();
		  final HttpRequestDescriptor request = new HttpRequestDescriptor(uri, method,
		      parameters);
		  executor.execute(request);
		} catch(final UnsupportedEncodingException exception) { }
		  catch(final URISyntaxException exception) {
		  save(notify(context, Notification.ERROR, 21609, uri, method, URLEncodedUtils.format(parameters, "UTF-8"),
		        null, null));
		} catch(final ClientProtocolException exception) {
		  save(notify(context, Notification.ERROR, 11206, uri, method, URLEncodedUtils.format(parameters, "UTF-8"),
		      null, null));
		} catch(final IllegalArgumentException exception) { }
		  catch(final IOException exception) {
		    save(notify(context, Notification.ERROR, 11200, uri, method, URLEncodedUtils.format(parameters, "UTF-8"),
			    null, null));
		}
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
			    throw new VisitorException(exception);
			}
		}
	}
}
