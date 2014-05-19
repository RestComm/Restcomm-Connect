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
package org.mobicents.servlet.restcomm.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DiskCache extends UntypedActor {

    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final String location;
    private final String uri;

    public DiskCache(final String location, final String uri, final boolean create) {
        super();
        // Format the cache path.
        String temp = location;
        if (!temp.endsWith("/")) {
            temp += "/";
        }
        // Create the cache path if specified.
        final File path = new File(temp);
//        if (create) {
//            path.mkdirs();
//        }

        // Make sure the cache path exists and is a directory.
        if (!path.exists() || !path.isDirectory()) {
//            throw new IllegalArgumentException(location + " is not a valid cache location.");
            path.mkdirs();
        }
        // Format the cache URI.
        this.location = temp;
        temp = uri;
        if (!temp.endsWith("/")) {
            temp += "/";
        }
        this.uri = temp;
    }

    public DiskCache(final String location, final String uri) {
        this(location, uri, false);
    }

    private URI cache(final Object message) throws IOException {
        final DiskCacheRequest request = (DiskCacheRequest) message;

        if (request.hash() == null) {
            if (request.uri().getScheme().equalsIgnoreCase("file")) {
                File origFile = new File(request.uri());
                File destFile = new File(location + origFile.getName());
                if (!destFile.exists())
                    FileUtils.moveFile(origFile, destFile);

                return URI.create(this.uri + destFile.getName());

            } else {
                // This is a request to cache a URI
                String hash = null;
                URI uri = null;
                if (request.uri().toString().contains("hash")) {
                    String fragment = request.uri().getFragment();
                    hash = fragment.replace("hash=", "");
                    String uriStr = ((request.uri().toString()).replace(fragment, "")).replace("#", "");
                    uri = URI.create(uriStr);
                } else {
                    uri = request.uri();
                    hash = new Sha256Hash(uri.toString()).toHex();
                }

                final String extension = extension(uri).toLowerCase();
                final File path = new File(location + hash + "." + extension);
                if (!path.exists()) {
                    final File tmp = new File(path + "." + "tmp");
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        input = uri.toURL().openStream();
                        output = new FileOutputStream(tmp);
                        final byte[] buffer = new byte[4096];
                        int read = 0;
                        do {
                            read = input.read(buffer, 0, 4096);
                            if (read > 0) {
                                output.write(buffer, 0, read);
                            }
                        } while (read != -1);
                        tmp.renameTo(path);
                    } finally {
                        if (input != null) {
                            input.close();
                        }
                        if (output != null) {
                            output.close();
                        }
                    }
                }
                return URI.create(this.uri + hash + "." + extension);
            }
        } else {
            // This is a check cache request
            final String extension = "wav";
            final String hash = request.hash();
            final String filename = hash + "." + extension;
            File matchedFile = (new File(location)).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    // return name.startsWith(hash) && name.endsWith("."+extension);
                    return name.equalsIgnoreCase(filename);
                }
            })[0];

            if (matchedFile.exists()) {
                // return URI.create(matchedFile.getAbsolutePath());
                return URI.create(this.uri + filename);
            } else {
                throw new FileNotFoundException(filename);
            }
        }
    }

    private String extension(final URI uri) {
        final String path = uri.getPath();
        return path.substring(path.lastIndexOf(".") + 1);
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (DiskCacheRequest.class.equals(klass)) {
            DiskCacheResponse response = null;
            try {
                response = new DiskCacheResponse(cache(message));
            } catch (final Exception exception) {
                logger.error("Error while chaching", exception);
                response = new DiskCacheResponse(exception);
            }
            sender.tell(response, self);
        }
    }
}
