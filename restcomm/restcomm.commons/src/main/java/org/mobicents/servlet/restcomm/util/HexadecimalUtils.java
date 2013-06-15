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

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class HexadecimalUtils {
  private static final char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6',
    '7', '8', '9', 'a', 'b', 'c', 'd',
    'e', 'f'
  };
  
  private HexadecimalUtils() {
    super();
  }
  
  public static char[] toHex(final byte[] input) {
    int position = 0;
    char[] characters = new char[input.length * 2];
    for(int index = 0; index < input.length; index++) {
      characters[position++] = HEX_DIGITS[(input[index] >> 4) & 0x0F];
      characters[position++] = HEX_DIGITS[input[index] & 0x0f];
    }
    return characters;
  }
  
  public static String toHex(final String input) {
    return new String(toHex(input.getBytes()));
  }
}
