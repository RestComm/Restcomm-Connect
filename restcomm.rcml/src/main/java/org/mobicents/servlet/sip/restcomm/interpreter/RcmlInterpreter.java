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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
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
public final class RcmlInterpreter extends FiniteStateMachine implements Runnable, TagVisitor {
	// RcmlInterpreter states.
	private static final State IDLE = new State("idle");
	private static final State REDIRECTED = new State("redirected");
	private static final State READY = new State("ready");
	private static final State EXECUTING = new State("executing");
	private static final State FINISHED = new State("finished");
	private static final State FAILED = new State("failed");
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

	// Configuration
	private final Configuration configuration;
	// Data Access Object Manager.
	private final DaoManager daos;
	// RcmlInterpreter environment.
	private final RcmlInterpreterContext context;
	//Tag strategy strategies.
	private final TagStrategyFactory strategies;
	// XML Resource Builder.
	private final RcmlDocumentBuilder resourceBuilder;
	// XML Resource.
	private RcmlDocument resource;
	//XML Resource URI.
	private URI resourceUri;
	// XML Resource request attributes.
	private String requestVariables;
	// XML Resource request requestMethod.
	private String requestMethod;
	// XML Resource response body.
	private String responseBody;
	// XML Resource response headers.
	private String responseHeaders;

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
		final ServiceLocator services = ServiceLocator.getInstance();
		configuration = services.get(Configuration.class);
		daos = services.get(DaoManager.class);
	}
	
	private void checkContentType(final String type) throws InterpreterException {
		if(!type.contains("text/xml") && !type.contains("application/xml") && !type.contains("text/html")) {
			throw new InterpreterException("Invalid content type " + type);
		}
	}

	private void cleanup() {
		final Call call = context.getCall();
		if(Call.Status.IN_PROGRESS == call.getStatus()) {
			call.hangup();
		}
		sendStatusCallback();
	}

	public void failed() {
		assertState(EXECUTING);
		setState(FAILED);
	}

	private HttpResponse fetch(final URI uri, final String method, List<NameValuePair> variables)
			throws InterpreterException {
		final HttpUriRequest request = request(uri, method, variables);
		final HttpClient client = new DefaultHttpClient();
		try {
			return client.execute(request);
		} catch(final ClientProtocolException exception) {
			notify(context, Notification.ERROR, 11206, uri, method, getQueryString(getVariables(variables)));
			throw new InterpreterException(exception);
		} catch(final IOException exception) {
			notify(context, Notification.ERROR, 11200, uri, method, getQueryString(getVariables(variables)));
			throw new InterpreterException(exception);
		}
	}

	public void finish() {
		final State state = getState();
		if(!FINISHED.equals(state) && !FAILED.equals(state)) {
		  setState(FINISHED);
		}
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

	public void initialize() throws InterpreterException {
		try {
			load(context.getVoiceUrl(), context.getVoiceMethod());
		} catch(final InterpreterException exception) {
			load(context.getVoiceFallbackUrl(), context.getVoiceFallbackMethod());
		}
		setState(READY);
	}

	public void load(final URI uri, final String method) throws InterpreterException {
		load(uri, method, EMPTY_NAME_VALUE_PAIRS);
	}

	public void load(final URI uri, final String method, List<NameValuePair> variables)
			throws InterpreterException {
		final List<State> possibleStates = new ArrayList<State>();
		possibleStates.add(IDLE);
		possibleStates.add(EXECUTING);
		assertState(possibleStates);
		// Load the XML resource for execution.
		final String queryString = getQueryString(getVariables(variables));
		try {
			final HttpResponse response = fetch(uri, method, variables);
			validate(uri, method, queryString, response);
			final int status = response.getStatusLine().getStatusCode();
			if(status == HttpStatus.SC_OK) {
				final String body = StringUtils.toString(response.getEntity().getContent());
				final RcmlDocument document = resourceBuilder.build(body);
				validate(uri, method, queryString, document);
				requestMethod = method;
				requestVariables = queryString;
				responseHeaders = HttpUtils.toString(response.getAllHeaders());
				responseBody = body;
				resource = resourceBuilder.build(body);
				resourceUri = uri;
			} else {
				notify(context, Notification.ERROR, 11200, uri, method, queryString);
			}
		} catch(final IOException exception) {
			notify(context, Notification.ERROR, 11200, uri, method, queryString);
			throw new InterpreterException(exception);
		} catch(final RcmlDocumentBuilderException exception) {
			notify(context, Notification.ERROR, 12100, uri, method, queryString);
			throw new InterpreterException(exception);
		}
	}

	public void notify(final RcmlInterpreterContext context, final int log, final int errorCode) {
		notify(context, log, errorCode, resourceUri, requestMethod, requestVariables, responseBody,
				responseHeaders);
	}

	public void notify(final RcmlInterpreterContext context, final int log, final int errorCode,
			final URI resourceUri, final String requestMethod, final String requestVariables) {
		notify(context, log, errorCode, resourceUri, requestMethod, requestVariables, null, null);
	}

	public void notify(final RcmlInterpreterContext context, final int log, final int errorCode,
			final URI resourceUri, final String requestMethod, final String requestVariables,
			final String responseBody, final String responseHeaders) {
		final Notification.Builder builder = Notification.builder();
		final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
		builder.setSid(sid);
		builder.setAccountSid(context.getAccountSid());
		builder.setCallSid((Sid) context.getCall().getSid());
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
		final Notification notification = builder.build();
		final NotificationsDao dao = daos.getNotificationsDao();
		dao.addNotification(notification);
	}

	public void redirect() {
		assertState(EXECUTING);
		setState(REDIRECTED);
	}

	private String getQueryString(final List<NameValuePair> variables) {
		return URLEncodedUtils.format(variables, "UTF-8");
	}

	private List<NameValuePair> getVariables(final List<NameValuePair> variables) {
		final List<NameValuePair> result = context.getRcmlRequestParameters();
		result.addAll(variables);
		return result;
	}

	private HttpUriRequest request(final URI uri, final String method, List<NameValuePair> variables)
			throws InterpreterException {
		final List<NameValuePair> allVariables = getVariables(variables);
		final String queryString = getQueryString(allVariables);
		HttpUriRequest request = null;
		try {
			if("GET".equalsIgnoreCase(method)) {
				request = new HttpGet(URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(),
						queryString, null));
			} else if("POST".equalsIgnoreCase(method)) {
				final HttpPost post = new HttpPost(uri);
				post.setEntity(new UrlEncodedFormEntity(allVariables));
				request = post;
			}
		} catch(final URISyntaxException exception) {
			notify(context, Notification.ERROR, 11100, uri, method, queryString);
			throw new InterpreterException(exception);
		} catch(final UnsupportedEncodingException ignored) { }
		return request;
	}

	public void run() {
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
					catch(final VisitorException ignored) { /* Handled in tag strategy. */ }
					tag.setHasBeenVisited(true);
					// Make sure the call is still in progress.
					final Call call = context.getCall();
					if((Call.Status.RINGING != call.getStatus() && Call.Status.IN_PROGRESS != call.getStatus()))
					{ setState(FINISHED); }
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
			cleanup();
			finish();
		}
	}
	
	public void sendStatusCallback() {
		final URI uri = context.getStatusCallback();
		if(uri != null) {
			final String method = context.getStatusCallbackMethod();
			final HttpUriRequest request = request(uri, method, EMPTY_NAME_VALUE_PAIRS);
			final HttpClient client = new DefaultHttpClient();
			try {
				client.execute(request);
			} catch(final Exception exception) {
				throw new InterpreterException(exception);
			}
		}
	}

	private void validate(final URI uri, final String method, final String queryString,
			final HttpResponse response) throws InterpreterException {
		try {
			checkContentType(response.getFirstHeader("Content-Type").getValue());
		} catch(final InterpreterException exception) {
			notify(context, Notification.ERROR, 12300, uri, method, queryString);
			throw new InterpreterException(exception);
		}
	}

	private void validate(final URI uri, final String method, final String queryString,
			final Tag tag) throws InterpreterException {
		if(!"Response".equals(tag.getName())) {
			notify(context, Notification.ERROR, 12102, uri, method, queryString);
			throw new InterpreterException();
		}
	}

	public void visit(final Tag tag) throws VisitorException {
		if(tag instanceof RcmlTag) {
			final RcmlTag rcmlTag = (RcmlTag)tag;
			try {
				final TagStrategy strategy = strategies.getTagStrategyInstance(tag.getName());
				strategy.initialize(this, context, rcmlTag);
				strategy.execute(this, context, rcmlTag);
			} catch(final Exception exception) { /* Handled in tag strategy. */ }
		}
	}
}
