package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public final class MgcpLinkException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MgcpLinkException() {
    super();
  }

  public MgcpLinkException(final String message) {
    super(message);
  }

  public MgcpLinkException(final Throwable cause) {
    super(cause);
  }

  public MgcpLinkException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
