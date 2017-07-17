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
package org.restcomm.connect.commons.amazonS3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class S3AccessTool {

    private static Logger logger = Logger.getLogger(S3AccessTool.class);

    private String accessKey;
    private String securityKey;
    private String bucketName;
    private String folder;
    private String bucketRegion;
    private boolean reducedRedundancy;
    private int minutesToRetainPublicUrl;
    private boolean removeOriginalFile;
    private boolean testing;
    private String testingUrl;
    private AmazonS3 s3client;

    public S3AccessTool(final String accessKey, final String securityKey, final String bucketName, final String folder,
            final boolean reducedRedundancy, final int minutesToRetainPublicUrl, final boolean removeOriginalFile,
                        final String bucketRegion, final boolean testing, final String testingUrl) {
        this.accessKey = accessKey;
        this.securityKey = securityKey;
        this.bucketName = bucketName;
        this.folder = folder;
        this.reducedRedundancy = reducedRedundancy;
        this.minutesToRetainPublicUrl = minutesToRetainPublicUrl;
        this.removeOriginalFile = removeOriginalFile;
        this.bucketRegion = bucketRegion;
        this.testing = testing;
        this.testingUrl = testingUrl;
    }

    public AmazonS3 getS3client() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, securityKey);

        if (testing && (!testingUrl.isEmpty() || !testingUrl.equals(""))) {
            s3client = new AmazonS3Client(awsCreds);
            s3client.setRegion(Region.getRegion(Regions.fromName(bucketRegion)));
            s3client.setEndpoint(testingUrl);
            s3client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).disableChunkedEncoding().build());
        } else {
            s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.fromName(bucketRegion))
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        }

        return s3client;
    }

    public URI uploadFile(final String fileToUpload) {
        if (s3client == null) {
            s3client = getS3client();
        }
        if(logger.isInfoEnabled()){
            logger.info("S3 Region: "+bucketRegion.toString());
        }
        try {
            URI fileUri = URI.create(fileToUpload);
            File file = new File(fileUri);
            if (!file.exists() && testing && (!testingUrl.isEmpty() || !testingUrl.equals(""))) {
//                s3client.setEndpoint(testingUrl);
//                s3client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
                FileUtils.touch(new File(URI.create(fileToUpload)));
            }
            StringBuffer bucket = new StringBuffer();
            bucket.append(bucketName);
            if (folder != null && !folder.isEmpty())
                bucket.append("/").append(folder);
            if (logger.isInfoEnabled()) {
                logger.info("File to upload to S3: " + fileUri.toString());
            }

            while (!FileUtils.waitFor(file, 30)){}
            if (testing) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Will thread sleep for 1 minute simulating the long operation of FileUtils.waitFor");
                    }
                    Thread.sleep(60000);
                } catch (Exception e) {
                    logger.error("Exception while sleepig simulating the long operation waiting for the file");

                }
            }
            if (file.exists()) {
                PutObjectRequest putRequest = new PutObjectRequest(bucket.toString(), file.getName(), file);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(new MimetypesFileTypeMap().getContentType(file));
                putRequest.setMetadata(metadata);
                if (reducedRedundancy)
                    putRequest.setStorageClass(StorageClass.ReducedRedundancy);
                s3client.putObject(putRequest);

                if (removeOriginalFile) {
                    removeLocalFile(file);
                }
                URI recordingS3Uri = s3client.getUrl(bucket.toString(), file.getName()).toURI();
                return recordingS3Uri;
//                return downloadUrl.toURI();
            } else {
                logger.error("Timeout waiting for the recording file: "+file.getAbsolutePath());
                return null;
            }
         } catch (AmazonServiceException ase) {
            logger.error("Caught an AmazonServiceException");
            logger.error("Error Message:    " + ase.getMessage());
            logger.error("HTTP Status Code: " + ase.getStatusCode());
            logger.error("AWS Error Code:   " + ase.getErrorCode());
            logger.error("Error Type:       " + ase.getErrorType());
            logger.error("Request ID:       " + ase.getRequestId());
            return null;
        } catch (AmazonClientException ace) {
            logger.error("Caught an AmazonClientException ");
            logger.error("Error Message: " + ace.getMessage());
            return null;
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException: "+e.getMessage());
            return null;
        } catch (IOException e) {
            logger.error("Problem while trying to touch recording file for testing", e);
            return null;
        }
    }

    public URI getPublicUrl (String fileName) throws URISyntaxException {
        if (s3client == null) {
            s3client = getS3client();
        }
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, minutesToRetainPublicUrl);
        if (logger.isInfoEnabled()) {
            final String msg = String.format("Prepared amazon s3 public url valid for %s minutes for recording: %s",minutesToRetainPublicUrl, fileName);
            logger.info(msg);
        }
        date = cal.getTime();
        String bucket = bucketName;
        if (folder != null && !folder.isEmpty()) {
            bucket = bucket.concat("/").concat(folder);
        }
        GeneratePresignedUrlRequest generatePresignedUrlRequestGET =
                new GeneratePresignedUrlRequest(bucket, fileName);
        generatePresignedUrlRequestGET.setMethod(HttpMethod.GET);
        generatePresignedUrlRequestGET.setExpiration(date);

        return s3client.generatePresignedUrl(generatePresignedUrlRequestGET).toURI();
    }

    private void removeLocalFile(final File file) {
        if (!file.delete()) {
            if(logger.isInfoEnabled()){
                logger.info("Error while trying to delete the file: "+file.toString());
            }
        }
    }
}
