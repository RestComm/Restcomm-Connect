package org.restcomm.connect.commons.cache;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.restcomm.connect.commons.common.http.CustomHttpClientBuilder;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Gennadiy Dubina
 */
public class FileDownloader {

public URI download(URI requestUri, File pathToSave) throws IOException, URISyntaxException {
        final File tmp = new File(pathToSave + "." + "tmp");
        InputStream input = null;
        OutputStream output = null;
        HttpClient client = null;
        HttpResponse httpResponse = null;
        try {
            if (requestUri.getScheme().equalsIgnoreCase("https")) {
                //Handle the HTTPS URIs
                client = CustomHttpClientBuilder.buildDefaultClient(RestcommConfiguration.getInstance().getMain());
                /*URI result = new URIBuilder()
                        .setScheme(requestUri.getScheme())
                        .setHost(requestUri.getHost())
                        .setPort(requestUri.getPort())
                        .setPath(requestUri.getPath())
                        .build();
                HttpGet httpRequest = new HttpGet(result);
                */
                HttpGet httpRequest = new HttpGet(requestUri);
                httpResponse = client.execute(httpRequest);
                int code = httpResponse.getStatusLine().getStatusCode();

                if (code >= 400) {
                    String requestUrl = httpRequest.getRequestLine().getUri();
                    String errorReason = httpResponse.getStatusLine().getReasonPhrase();
                    String httpErrorMessage = String.format(
                            "Error while fetching http resource: %s \n Http error code: %d \n Http error message: %s",
                            requestUrl, code, errorReason);
                    throw new IOException(httpErrorMessage);
                }
                input = httpResponse.getEntity().getContent();
            } else {
                input = requestUri.toURL().openStream();
            }
            output = new FileOutputStream(tmp);
            final byte[] buffer = new byte[4096];
            int read = 0;
            do {
                read = input.read(buffer, 0, 4096);
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            } while (read != -1);
            tmp.renameTo(pathToSave);
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (httpResponse != null) {
                ((CloseableHttpResponse) httpResponse).close();
                httpResponse = null;
            }
        }

        return pathToSave.toURI();
    }
}
