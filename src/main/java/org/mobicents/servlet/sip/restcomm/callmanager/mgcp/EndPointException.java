package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public final class EndPointException extends Exception {
  private static final long serialVersionUID = 1L;

  public EndPointException() {
    super();
  }

  public EndPointException(final String message) {
    super(message);
  }

  public EndPointException(final Throwable cause) {
    super(cause);
  }
  
  public EndPointException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
