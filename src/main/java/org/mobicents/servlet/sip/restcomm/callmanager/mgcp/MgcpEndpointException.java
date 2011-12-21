package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public final class MgcpEndpointException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MgcpEndpointException() {
    super();
  }

  public MgcpEndpointException(final String message) {
    super(message);
  }

  public MgcpEndpointException(final Throwable cause) {
    super(cause);
  }

  public MgcpEndpointException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
