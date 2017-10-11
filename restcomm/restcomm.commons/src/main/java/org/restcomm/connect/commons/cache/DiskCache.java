/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.commons.cache;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DiskCache extends RestcommUntypedActor {
    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final String cacheDir;
    private final String cacheUri;

    // flag for cache disabling in *.wav files usage case
    private boolean wavNoCache = false;
    private FileDownloader downloader;

    public DiskCache(FileDownloader downloader, String cacheDir, String cacheUri, final boolean create, final boolean wavNoCache) {
        super();

        this.wavNoCache = wavNoCache;
        this.downloader = downloader;

        // Format the cache path.
        if (!cacheDir.endsWith("/")) {
            cacheDir += "/";
        }
        // Create the cache path if specified.
        final File path = new File(cacheDir);
        //        if (create) {
        //            path.mkdirs();
        //        }

        // Make sure the cache path exists and is a directory.
        if (!path.exists() || !path.isDirectory()) {
            //            throw new IllegalArgumentException(cacheDir + " is not a valid cache cacheDir.");
            path.mkdirs();
        }
        // Format the cache URI.
        this.cacheDir = cacheDir;
        if (!cacheUri.endsWith("/")) {
            cacheUri += "/";
        }
        this.cacheUri = cacheUri;
    }

    public DiskCache(FileDownloader downloader, final String cacheDir, final String cacheUri, final boolean create) {
        this(downloader, cacheDir, cacheUri, create, false);
    }

    public DiskCache(FileDownloader downloader, final String cacheDir, final String cacheUri) {
        this(downloader, cacheDir, cacheUri, false);
    }

    // constructor for compatibility with existing tests
    public DiskCache(final String cacheDir, final String cacheUri, final boolean create) {
        this(new FileDownloader(), cacheDir, cacheUri, create, false);
    }

    public URI cache(final DiskCacheRequest request) throws IOException, URISyntaxException {
        if (StringUtils.isNotEmpty(request.hash())) {
            return handleHashedRequest(request);
        } else if ("file".equalsIgnoreCase(request.uri().getScheme())) {
            return handleLocalFile(request);
        } else {
            return handleExternalUrl(request);
        }
    }

    private URI handleHashedRequest(final DiskCacheRequest request) throws FileNotFoundException {
        // This is a check cache request
        final String extension = "wav";
        final String hash = request.hash();
        final String filename = hash + "." + extension;
        Path p = Paths.get(cacheDir + filename);

        if (Files.exists(p)) {
            // return URI.create(matchedFile.getAbsolutePath());
            return URI.create(this.cacheUri + filename);
        } else {
            throw new FileNotFoundException("File "+filename+" NotFound");
        }
    }

    private URI handleLocalFile(final DiskCacheRequest request) throws IOException {
        File origFile = new File(request.uri());
        File destFile = new File(cacheDir + origFile.getName());
        if (!destFile.exists()) {
            FileUtils.moveFile(origFile, destFile);
        }
        return URI.create(this.cacheUri + destFile.getName());
    }

    private URI handleExternalUrl(final DiskCacheRequest request) throws IOException, URISyntaxException {
        //Handle all the rest
        // This is a request to cache a URI
        String hash;
        URI uri;
        URI requestUri = request.uri();
        String requestUriText = requestUri.toString();
        if (wavNoCache && "wav".equalsIgnoreCase(extension(requestUri))) {
            return requestUri;
        }else if (requestUriText.contains("hash")) {
            String fragment = requestUri.getFragment();
            hash = fragment.replace("hash=", "");
            String uriStr = requestUriText.replace(fragment, "").replace("#", "");
            uri = URI.create(uriStr);
        } else {
            uri = requestUri;
            hash = new Sha256Hash(requestUriText).toHex();
        }

        final String extension = extension(uri).toLowerCase();
        File path = null;
        if (extension.equalsIgnoreCase("wav")) {
            path = new File(cacheDir + hash + "." + extension);
            if (!path.exists()) {
                downloader.download(uri, path);
            }
            return URI.create(this.cacheUri + hash + "." + extension);
        } else {
            path = new File(cacheDir + hash + ".wav" );
            if (!path.exists()) {
                downloader.download(uri, path);
            }
            return URI.create(this.cacheUri + hash + ".wav");
        }
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (!(message instanceof DiskCacheRequest)) {
            logger.warning("Unexpected request type");
            return;
        }
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (DiskCacheRequest.class.equals(klass)) {
            DiskCacheResponse response;
            try {
                response = new DiskCacheResponse(cache((DiskCacheRequest) message));
            } catch (final Exception exception) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Issue while caching", exception);
                }
                response = new DiskCacheResponse(exception);
            }
            sender.tell(response, self);
        }
    }

    private static String extension(final URI uri) {
        final String path = uri.getPath();
        return path.substring(path.lastIndexOf(".") + 1);
    }

}
