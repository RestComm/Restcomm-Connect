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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class StringUtils {
    private static final Pattern numberPattern = Pattern.compile("\\d+");

    private StringUtils() {
        super();
    }

    public static String addSuffixIfNotPresent(final String text, final String suffix) {
        if (text.endsWith(suffix)) {
            return text;
        } else {
            return text + suffix;
        }
    }

    public static boolean isPositiveInteger(final String text) {
        return numberPattern.matcher(text).matches();
    }

    public static String toString(final InputStream input) throws IOException {
        final InputStreamReader reader = new InputStreamReader(input);
        final StringWriter writer = new StringWriter();
        final char[] data = new char[512];
        int bytesRead = -1;
        do {
            bytesRead = reader.read(data);
            if (bytesRead > 0) {
                writer.write(data, 0, bytesRead);
            }
        } while (bytesRead != -1);
        return writer.getBuffer().toString();
    }
}
