package org.mobicents.servlet.restcomm.smpp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipServletRequest;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.patterns.StandardResponse;

import akka.actor.ActorRef;


public class SmppSessionObjects {


    @Immutable
    public final class SmppSessionRequest {
        private final String from;
        private final String to;
        private final String body;
        private final SipServletRequest origRequest;
        private final ConcurrentHashMap<String, String> customHeaders;

        //TODO need to check which is using the SmsSessionRequest and modify accordingly to include or not the custom headers
        public SmppSessionRequest(final String from, final String to, final String body, final SipServletRequest origRequest, final ConcurrentHashMap<String, String> customHeaders) {
            super();
            this.from = from;
            this.to = to;
            this.origRequest = origRequest;
            this.body = body;
            this.customHeaders = customHeaders;
        }

        public SmppSessionRequest(final String from, final String to, final String body, final ConcurrentHashMap<String, String> customHeaders) {
            this(from, to, body, null, customHeaders);
        }

        public String from() {
            return from;
        }

        public String to() {
            return to;
        }

        public String body() {
            return body;
        }

        public SipServletRequest getOrigRequest() {
            return origRequest;
        }

        public ConcurrentHashMap<String, String> headers() {
            return customHeaders;
        }
    }

    @Immutable
    public final class GetLastSmppRequest {
        public GetLastSmppRequest() {
            super();
        }
    }


    @Immutable
    public final class SmppSessionAttribute {
        private final String name;
        private final Object value;

        public SmppSessionAttribute(final String name, final Object value) {
            super();
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public Object value() {
            return value;
        }
    }

    public final class SmppSessionResponse {
        private final SmppSessionInfo info;
        private final boolean success;

        public SmppSessionResponse(final SmppSessionInfo info, final boolean success) {
            super();
            this.info = info;
            this.success = success;
        }

        public SmppSessionInfo info() {
            return info;
        }

        public boolean succeeded() {
            return success;
        }
    }

    @Immutable
    public final class SmppSessionInfo {
        private final String from;
        private final String to;
        private final Map<String, Object> attributes;

        public SmppSessionInfo(final String from, final String to, final Map<String, Object> attributes) {
            super();
            this.from = from;
            this.to = to;
            this.attributes = attributes;
        }

        public String from() {
            return from;
        }

        public String to() {
            return to;
        }

        public Map<String, Object> attributes() {
            return attributes;
        }
    }

    @Immutable
    public final class SmppServiceResponse<T> extends StandardResponse<T> {
        public SmppServiceResponse(final T object) {
            super(object);
        }

        public SmppServiceResponse(final Throwable cause, final String message) {
            super(cause, message);
        }

        public SmppServiceResponse(final Throwable cause) {
            super(cause);
        }
    }

    @Immutable
    public final class SmppStartInterpreter {
        private final ActorRef resource;

        public SmppStartInterpreter(final ActorRef resource) {
            super();
            this.resource = resource;
        }

        public ActorRef resource() {
            return resource;
        }
    }
    @Immutable
    public final class SmppStopInterpreter {

        private final boolean liveCallModification;

        public SmppStopInterpreter(boolean liveCallModification) {
            this.liveCallModification = liveCallModification;
        }

        public SmppStopInterpreter() {
            this(false);
        }

        public boolean isLiveCallModification() {
            return liveCallModification;
        }

    }

    @Immutable
    public final class CreateSmppSession {
        public CreateSmppSession() {
            super();
        }
    }

    @Immutable
    public final class DestroySmppSession {
        private final ActorRef session;

        public DestroySmppSession(final ActorRef session) {
            super();
            this.session = session;
        }

        public ActorRef session() {
            return session;
        }
    }

}
