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
package org.mobicents.servlet.sip.restcomm.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.HexadecimalUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class DiskCache {
  private static final String FILE_EXTENSION = "wav";  
  private final String location;
  
  public DiskCache(final String location) throws IllegalArgumentException {
    super();
    String temp = location;
    if(!temp.endsWith("/")) {
      temp += "/";
    }
    final File path = new File(temp);
    if(!path.exists() || !path.isDirectory()) {
      throw new IllegalArgumentException(location + " is not a valid cache location.");
    }
    this.location = temp;
  }
  
  private String buildPath(final String key) {
	final StringBuilder buffer = new StringBuilder();
	buffer.append(location).append(hash(key)).append(".").append(FILE_EXTENSION);
    return buffer.toString();
  }
  
  public boolean contains(final String key) {
	final String path = buildPath(key);
	final File file = new File(path);
    return file.exists();
  }
  
  public URI get(final String key) {
	if(contains(key)) {
	  return toUri(buildPath(key));
	} else {
      return null;
	}
  }
  
  private String hash(final String key) {
    MessageDigest messageDigest = null;
	try {
	  messageDigest = MessageDigest.getInstance("SHA-256");
	} catch(final NoSuchAlgorithmException ignored) { }
	messageDigest.update(key.getBytes());
	final byte[] hash = messageDigest.digest();
	return new String(HexadecimalUtils.toHex(hash));
  }
  
  public URI put(final String key, final InputStream data) throws IOException {
    if(contains(key)) {
      return get(key);
    } else {
      final String path = buildPath(key);
      RandomAccessFile file = null;
  	  FileChannel channel = null;
  	  FileLock lock = null;
      try {
        // Create a new file.
        file = new RandomAccessFile(new File(path), "rw");
        channel = file.getChannel();
        // Acquire a lock to the file.
        lock = channel.lock();
        if(contains(key)) {
          return get(key);
        }
        // Write the data to the file.
        final byte[] dataBuffer = new byte[8192];
        final ByteBuffer channelBuffer = ByteBuffer.allocate(8192);
        int bytesRead = 0;
        do {
          bytesRead = data.read(dataBuffer);
          if(bytesRead > 0) {
            channelBuffer.put(dataBuffer, 0, bytesRead);
            channel.write(channelBuffer);
            channelBuffer.clear();
          }
        } while(bytesRead != -1);
        // return a URI to the file.
        return toUri(path);
      } finally {
    	// Release the lock.
    	if(lock != null && lock.isValid()) {
          lock.release();
    	}
    	// Close the file.
    	if(file != null) {
          file.close();
    	}
      }
    }
  }
  
  private URI toUri(final String file) {
    return URI.create("file://" + file);
  }
}
