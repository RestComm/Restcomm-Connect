package org.mobicents.servlet.sip.restcomm.util;

public final class WrapAroundCounter {
  private volatile long count;
  private final long limit;
  
  public WrapAroundCounter(final long limit) {
	if(limit <= 0) {
	  throw new IllegalArgumentException("The counter limit can not be less than or equal to 0");
	}
	this.count = 0;
    this.limit = limit;
  }
  
  public long get() {
    return count;
  }
  
  public synchronized long getAndIncrement() {
    final long result = count;
    increment();
    return result;
  }
  
  public synchronized void increment() {
    count += 1;
    if(count == limit) {
      count = 0;
    }
  }
}
