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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.HexadecimalUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class DiskCache {
  private final String location;
  private final Map<String, Lock> locks;
  
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
    this.locks = new ConcurrentHashMap<String, Lock>();
  }
  
  private String buildPath(final String key, final String extension) {
	final StringBuilder buffer = new StringBuilder();
	buffer.append(location).append(hash(key)).append(".").append(extension.toLowerCase());
    return buffer.toString();
  }
  
  public boolean contains(final String key, final String extension) {
    final String path = buildPath(key, extension);
	final File file = new File(path);
    if(file.exists()) {
      return true;
    } else {
      return false;
    }
  }
  
  public URI get(final String key, final String extension) {
    if(contains(key, extension)) {
      return toUri(buildPath(key, extension));
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
  
  private String getFileExtension(final URI uri) {
	final String path = uri.getPath();
    return path.substring(path.lastIndexOf(".") + 1);
  }
  
  public URI put(final String key, final URI uri) throws IOException {
	final String extension = getFileExtension(uri);
	final String path = buildPath(key, extension);
	final File file = new File(path);
    if(file.exists()) {
	  return toUri(path);
    } else {
      final Lock lock = new ReentrantLock();
      locks.put(file.toString(), lock);
      lock.lock();
      if(!file.exists()) {
        try {
          final InputStream data = new BufferedInputStream(uri.toURL().openStream());
          return put(data, file);
        } finally {
          lock.unlock();
        }
      } else {
        return toUri(path);
      }
    }
  }
  
  private URI put(final InputStream data, final File file) throws IOException {
	final File temporaryFile = new File(file.getPath() + ".tmp");
    FileOutputStream output = null;
    try {
      // Create a new file.
      output = new FileOutputStream(temporaryFile);
      // Write the data to the file.
      final byte[] dataBuffer = new byte[8192];
      int bytesRead = 0;
      do {
        bytesRead = data.read(dataBuffer, 0, 8192);
        if(bytesRead > 0) {
          output.write(dataBuffer, 0, bytesRead);
        }
      } while(bytesRead != -1);
      // return a URI to the file.
      temporaryFile.renameTo(file);
      return toUri(file.getPath());
    } finally {
      // Close the output stream.
      if(output != null) {
        output.close();
      }
      // Close the input stream.
      data.close();
    }
  }
  
  private URI toUri(final String file) {
    return URI.create("file://" + file);
  }
}
