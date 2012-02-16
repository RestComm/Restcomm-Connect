package org.mobicents.servlet.sip.restcomm;

import java.util.UUID;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

@Immutable public final class Sid {
  private final String id;
  
  public Sid() {
    super();
    id = "AC" + UUID.randomUUID().toString().replaceAll("-", "");
  }

  @Override public boolean equals(Object object) {
	if(this == object) {
	  return true;
	}
	if(object == null) {
	  return false;
	}
	if(getClass() != object.getClass()) {
	  return false;
	}
	final Sid other = (Sid)object;
	if(!toString().equals(other.toString())) {
	  return false;
	}
	return true;
  }

  @Override public int hashCode() {
	final int prime = 5;
	int result = 1;
	result = prime * result + id.hashCode();
	return result;
  }

  @Override public String toString() {
    return id;
  }
}
