package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public class MgcpConnectionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MgcpConnectionException() {
    super();
  }

  public MgcpConnectionException(final String message) {
    super(message);
  }

  public MgcpConnectionException(final Throwable cause) {
    super(cause);
  }

  public MgcpConnectionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
