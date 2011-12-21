package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public final class MgcpServerException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MgcpServerException() {
    super();
  }

  public MgcpServerException(final String message) {
    super(message);
  }

  public MgcpServerException(final Throwable cause) {
    super(cause);
  }

  public MgcpServerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
