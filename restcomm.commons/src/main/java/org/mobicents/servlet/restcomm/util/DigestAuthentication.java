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
package org.mobicents.servlet.restcomm.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class DigestAuthentication {
  private DigestAuthentication() {
    super();
  }
  
  private static String A1(final String algorithm, final String user, final String realm, final String password,
      final String nonce, final String cnonce) {
    if(algorithm == null || algorithm.trim().length() == 0 || algorithm.trim().equalsIgnoreCase("MD5")) {
      return user + ":" + realm + ":" + password;
    } else {
      if(cnonce == null || cnonce.length() == 0) {
        throw new NullPointerException("The cnonce parameter may not be null.");
      }
      return H(user + ":" + realm + ":" + password) + ":" + nonce + ":" + cnonce;
    }
  }
  
  private static String A2(final String method, final String uri, String body, final String qop) {
    if(qop == null || qop.trim().length() == 0 || qop.trim().equalsIgnoreCase("auth")) {
      return method + ":" + uri;
    } else {
      if(body == null)
      body = "";
      return method + ":" + uri + ":" + H(body);
    }
  }
  
  public static String response(final String algorithm, final String user, final String realm, final String password,
      final String nonce, final String nc, final String cnonce, final String method, final String uri, String body, final String qop) {
	validate(user, realm, password, nonce, method, uri);
    final String a1 = A1(algorithm, user, realm, password, nonce, cnonce);
    final String a2 = A2(method, uri, body, qop);
    if(cnonce != null && qop != null && nc != null && (qop.equalsIgnoreCase("auth") || qop.equalsIgnoreCase("auth-int"))) {
      return KD( H(a1), nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + H(a2));
    } else {
      return KD(H(a1), nonce + ":" + H(a2));
    }
  }

  private static String H(final String data) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("MD5");
      final byte[] result = digest.digest(data.getBytes());
      final char[] characters = HexadecimalUtils.toHex(result);
      return new String(characters);
    } catch(final NoSuchAlgorithmException exception) {
      return null;
    }
  }

  private static String KD(final String secret, final String data) {
    return H(secret + ":" + data);
  }
  
  private static void validate(final String user, final String realm, final String password, final String nonce,
      final String method, final String uri) {
    if(user == null) {
      throw new NullPointerException("The user parameter may not be null.");
    } else if(realm == null) {
      throw new NullPointerException("The realm parameter may not be null.");
    } else if(password == null) {
      throw new NullPointerException("The password parameter may not be null.");
    } else if(method == null) {
      throw new NullPointerException("The method parameter may not be null.");
    } else if(uri == null) {
      throw new NullPointerException("The uri parameter may not be null.");
    } else if(nonce == null) {
      throw new NullPointerException("The nonce parameter may not be null.");
    }
  }
}
